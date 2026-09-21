package dev.piovra.connector.woocommerce.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.Money;
import dev.piovra.common.TenantId;
import dev.piovra.connector.woocommerce.application.port.out.CredentialsResolver;
import dev.piovra.connector.woocommerce.application.port.out.PollCursorStore;
import dev.piovra.connector.woocommerce.application.port.out.PollKind;
import dev.piovra.connector.woocommerce.application.port.out.RawPayloadStore;
import dev.piovra.connector.woocommerce.application.service.OrderPollingService;
import dev.piovra.driver.spi.ChannelContext;
import dev.piovra.driver.spi.ChannelCredentials;
import dev.piovra.driver.spi.MarketplaceDriver;
import dev.piovra.driver.spi.OrderPage;
import dev.piovra.driver.spi.OrderQuery;
import dev.piovra.driver.spi.RemoteOrder;
import dev.piovra.events.Topics;
import dev.piovra.model.channel.ChannelDefinition;
import dev.piovra.model.channel.ChannelType;
import dev.piovra.model.order.Address;
import dev.piovra.model.order.Buyer;
import dev.piovra.model.order.CanonicalOrder;
import dev.piovra.model.order.LineResolution;
import dev.piovra.model.order.OrderLine;
import dev.piovra.model.order.OrderStatus;
import dev.piovra.model.order.OrderTotals;
import dev.piovra.testsupport.PiovraIntegrationTest;
import dev.piovra.testsupport.PiovraKafkaContainer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The polling loop against a scripted driver. The subject is the relationship between what was read
 * and where the cursor ended up - not HTTP, which {@code WooCommerceOrderFetchTest} covers, and not
 * object storage, which {@code S3RawPayloadStoreTest} covers.
 */
@Import(OrderPollingServiceTest.ScriptedDriverConfiguration.class)
class OrderPollingServiceTest extends PiovraIntegrationTest {

    private static final TenantId TENANT = TenantId.DEFAULT;

    @Autowired
    private OrderPollingService pollingService;

    @Autowired
    private PollCursorStore cursorStore;

    @Autowired
    private ScriptedDriver driver;

    @Autowired
    private InMemoryPayloadStore payloadStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private ChannelDefinition channel;

    @BeforeEach
    void seedChannel() {
        channel = channelDefinition(ChannelId.of("woo-" + Ids.newId().toLowerCase()));
        cursorStore.ensureExists(TENANT, channel.channelId(), PollKind.ORDERS, Instant.parse("2026-09-21T10:00:00Z"));
        driver.pages.clear();
        driver.failure = null;
        driver.calls = 0;
    }

    @Test
    void a_polled_order_reaches_channel_order_received_carrying_only_a_payload_uri() throws Exception {
        String channelOrderId = "ord-" + Ids.newId();
        driver.pages.add(lastPage(channelOrderId));

        pollingService.pollOnce(channel);

        try (KafkaConsumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(List.of(Topics.CHANNEL_ORDER_RECEIVED));
            List<String> received = new ArrayList<>();
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                consumer.poll(Duration.ofMillis(500)).forEach(record -> received.add(record.value()));
                assertThat(received).anyMatch(value -> isFor(value, channelOrderId));
            });
        }
    }

    @Test
    void the_event_references_the_raw_payload_instead_of_embedding_it() {
        String channelOrderId = "ord-" + Ids.newId();
        driver.pages.add(lastPage(channelOrderId));

        pollingService.pollOnce(channel);

        String event = outboxPayload(channelOrderId);
        assertThat(event).contains(payloadStore.uris.get(channelOrderId));
        // The canonical order travels in full - the order service needs the address to fulfil. What
        // stays behind is the marketplace's own payload, with its payment details, notes and
        // plugin metadata: "billing" is a WooCommerce key, absent from the canonical model.
        assertThat(event).doesNotContain("billing");
    }

    @Test
    void the_cursor_advances_to_the_window_end_once_the_scan_finishes() {
        driver.pages.add(lastPage("ord-" + Ids.newId()));
        Instant before = Instant.now();

        pollingService.pollOnce(channel);

        assertThat(cursorAt()).isAfterOrEqualTo(before.minusSeconds(1));
        assertThat(pageCursor()).isNull();
    }

    @Test
    void a_scan_that_has_more_pages_keeps_the_window_and_remembers_the_driver_token() {
        // max-pages-per-tick is 1 in the test configuration, so the scan stops with the page
        // budget spent and the mid-scan state still on the row.
        driver.pages.add(new OrderPage(List.of(remoteOrder("ord-" + Ids.newId())), "1758448800:2", 50));

        pollingService.pollOnce(channel);

        assertThat(cursorAt()).isEqualTo(Instant.parse("2026-09-21T10:00:00Z"));
        assertThat(pageCursor()).isEqualTo("1758448800:2");
    }

    @Test
    void a_driver_failure_releases_the_lease_and_leaves_the_cursor_untouched() {
        driver.failure = new IllegalStateException("the store is down");

        pollingService.pollOnce(channel);

        assertThat(cursorAt()).isEqualTo(Instant.parse("2026-09-21T10:00:00Z"));
        assertThat(lastError()).isNotNull();
        // Free again, so the next tick retries instead of stalling the channel forever.
        assertThat(cursorStore.acquire(TENANT, channel.channelId(), PollKind.ORDERS, Duration.ofMinutes(5)))
                .isPresent();
    }

    @Test
    void a_channel_another_replica_holds_is_left_alone() {
        cursorStore.acquire(TENANT, channel.channelId(), PollKind.ORDERS, Duration.ofMinutes(5));
        driver.pages.add(lastPage("ord-" + Ids.newId()));

        pollingService.pollOnce(channel);

        assertThat(driver.calls).isZero();
    }

    @Test
    void polling_the_same_order_twice_emits_it_twice_and_never_moves_the_cursor_backwards() {
        String channelOrderId = "ord-" + Ids.newId();
        driver.pages.add(lastPage(channelOrderId));
        pollingService.pollOnce(channel);
        Instant afterFirst = cursorAt();

        // The overlap window means a marketplace legitimately hands the same order back. The
        // connector does not deduplicate - the order service does, on (channelId, channelOrderId).
        driver.pages.add(lastPage(channelOrderId));
        pollingService.pollOnce(channel);

        assertThat(outboxCount(channelOrderId)).isEqualTo(2);
        assertThat(cursorAt()).isAfterOrEqualTo(afterFirst);
    }

    private boolean isFor(String event, String channelOrderId) {
        JsonNode node = objectMapper.readTree(event);
        return channelOrderId.equals(node.path("channelOrderId").asString());
    }

    private Instant cursorAt() {
        return jdbcTemplate
                .queryForObject(
                        "SELECT cursor_at FROM connector_woocommerce.poll_cursor WHERE channel_id = ?",
                        Timestamp.class,
                        channel.channelId().value())
                .toInstant();
    }

    private String pageCursor() {
        return jdbcTemplate.queryForObject(
                "SELECT page_cursor FROM connector_woocommerce.poll_cursor WHERE channel_id = ?",
                String.class,
                channel.channelId().value());
    }

    private String lastError() {
        return jdbcTemplate.queryForObject(
                "SELECT last_error FROM connector_woocommerce.poll_cursor WHERE channel_id = ?",
                String.class,
                channel.channelId().value());
    }

    private String outboxPayload(String channelOrderId) {
        return jdbcTemplate.queryForObject(
                "SELECT payload::text FROM connector_woocommerce.outbox_event WHERE payload->>'channelOrderId' = ?",
                String.class,
                channelOrderId);
    }

    private int outboxCount(String channelOrderId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM connector_woocommerce.outbox_event WHERE payload->>'channelOrderId' = ?",
                Integer.class,
                channelOrderId);
    }

    private static OrderPage lastPage(String channelOrderId) {
        return OrderPage.last(List.of(remoteOrder(channelOrderId)));
    }

    private static RemoteOrder remoteOrder(String channelOrderId) {
        CanonicalOrder order = new CanonicalOrder(
                null,
                TENANT,
                ChannelId.of("woo-placeholder"),
                channelOrderId,
                OrderStatus.PAID,
                "processing",
                Instant.parse("2026-09-21T10:30:00Z"),
                Instant.parse("2026-09-21T10:31:00Z"),
                new Buyer("42", "Mario Rossi", "mario@test.it"),
                new Address("Mario Rossi", "Via Riservata 1", null, "Milano", "MI", "20100", "IT", null),
                new OrderTotals(Money.euro("19.90"), Money.euro("0.00"), Money.euro("0.00"), Money.euro("19.90")),
                List.of(new OrderLine(
                        "77", "77", "TSHIRT-BASE", null, LineResolution.UNMAPPED, 1, Money.euro("19.90"))),
                false);
        return new RemoteOrder(
                order, "{\"id\":\"" + channelOrderId + "\",\"billing\":{\"address_1\":\"Via Riservata 1\"}}");
    }

    private ChannelDefinition channelDefinition(ChannelId channelId) {
        return new ChannelDefinition(
                TENANT,
                channelId,
                ChannelType.WOOCOMMERCE,
                "https://store.test",
                true,
                "vault://woo",
                null,
                Map.of(),
                Map.of());
    }

    private KafkaConsumer<String, String> testConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                PiovraKafkaContainer.instance().getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + Ids.newId());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }

    @TestConfiguration
    static class ScriptedDriverConfiguration {

        @Bean
        @Primary
        ScriptedDriver scriptedDriver() {
            return new ScriptedDriver();
        }

        @Bean
        @Primary
        InMemoryPayloadStore inMemoryPayloadStore() {
            return new InMemoryPayloadStore();
        }

        /** The channel ids are minted per test, so configuration-backed credentials cannot cover them. */
        @Bean
        @Primary
        CredentialsResolver testCredentialsResolver() {
            return definition -> ChannelCredentials.basic("ck", "cs");
        }
    }

    /** Hands out pre-scripted pages, or throws, so the loop's behaviour is the only variable. */
    static class ScriptedDriver implements MarketplaceDriver {

        final Deque<OrderPage> pages = new ArrayDeque<>();
        RuntimeException failure;
        int calls;

        @Override
        public ChannelType type() {
            return ChannelType.WOOCOMMERCE;
        }

        @Override
        public dev.piovra.driver.spi.DriverCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public dev.piovra.driver.spi.UpsertResult upsertListing(
                ChannelContext ctx, dev.piovra.driver.spi.ListingRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public dev.piovra.driver.spi.UpdateResult updateInventory(
                ChannelContext ctx, List<dev.piovra.driver.spi.InventoryUpdate> updates) {
            throw new UnsupportedOperationException();
        }

        @Override
        public dev.piovra.driver.spi.UpdateResult updatePrice(
                ChannelContext ctx, List<dev.piovra.driver.spi.PriceUpdate> updates) {
            throw new UnsupportedOperationException();
        }

        @Override
        public dev.piovra.driver.spi.UpdateResult endListing(
                ChannelContext ctx, dev.piovra.driver.spi.EndListingRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OrderPage fetchOrders(ChannelContext ctx, OrderQuery query) {
            calls++;
            if (failure != null) {
                throw failure;
            }
            OrderPage page = pages.poll();
            return page == null ? OrderPage.last(List.of()) : page;
        }

        @Override
        public Optional<RemoteOrder> fetchOrder(ChannelContext ctx, String channelOrderId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public dev.piovra.driver.spi.ListingPage fetchListings(
                ChannelContext ctx, dev.piovra.driver.spi.ListingQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public dev.piovra.driver.spi.DriverError translate(Exception e) {
            return dev.piovra.driver.spi.DriverError.internal("scripted failure", e);
        }
    }

    /** Object storage is not the subject here: {@code S3RawPayloadStoreTest} exercises the real one. */
    static class InMemoryPayloadStore implements RawPayloadStore {

        final Map<String, String> uris = new HashMap<>();

        @Override
        public String store(TenantId tenantId, ChannelId channelId, String channelOrderId, String payload) {
            String uri = "s3://test-bucket/" + channelId.value() + "/" + channelOrderId + ".json";
            uris.put(channelOrderId, uri);
            return uri;
        }
    }
}
