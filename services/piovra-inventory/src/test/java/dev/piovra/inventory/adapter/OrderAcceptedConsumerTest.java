package dev.piovra.inventory.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.Money;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.events.OrderAccepted;
import dev.piovra.events.Topics;
import dev.piovra.inventory.adapter.out.persistence.StockLevelRepositoryAdapter;
import dev.piovra.inventory.domain.model.StockLevel;
import dev.piovra.model.order.LineResolution;
import dev.piovra.model.order.OrderLine;
import dev.piovra.testsupport.PiovraIntegrationTest;
import dev.piovra.testsupport.PiovraKafkaContainer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Inert-but-wired path: nothing publishes {@code OrderAccepted} in production yet, but this proves
 * the consumer applies it correctly once something does. */
class OrderAcceptedConsumerTest extends PiovraIntegrationTest {

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StockLevelRepositoryAdapter stockLevelRepository;

    @Test
    void an_order_accepted_message_decrements_stock_and_emits_inventory_changed() throws Exception {
        Sku sku = Sku.of("TEST-" + Ids.newId());
        StockLevel initial = inTransaction(() -> stockLevelRepository.lockOrCreate(TenantId.DEFAULT, sku));
        stockLevelRepository.save(new StockLevel(TenantId.DEFAULT, sku, 10, 0, 0, initial.version() + 1));

        OrderLine line = new OrderLine(
                "line-1", "channel-line-1", sku.value(), sku, LineResolution.MAPPED, 3, Money.euro("19.90"));
        OrderAccepted event = new OrderAccepted(
                Ids.newId(),
                TenantId.DEFAULT,
                "order-" + Ids.newId(),
                ChannelId.of("test-channel"),
                "channel-order-1",
                List.of(line),
                false,
                Instant.now());

        kafkaTemplate
                .send(Topics.ORDER_ACCEPTED, event.partitionKey(), objectMapper.writeValueAsString(event))
                .get();

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(
                        inTransaction(() -> stockLevelRepository.lockOrCreate(TenantId.DEFAULT, sku))
                                .onHand())
                .isEqualTo(7));

        try (KafkaConsumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(List.of(Topics.INVENTORY_CHANGED));
            // Accumulated across polls, and asserted on the values rather than on a boolean: when
            // this fails, the report has to show what was actually published.
            List<String> published = new ArrayList<>();
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                consumer.poll(Duration.ofMillis(500)).forEach(record -> published.add(record.value()));
                assertThat(published).anyMatch(value -> isTheExpectedInventoryChanged(value, sku));
            });
        }
    }

    /**
     * Parsed, not matched as a string: the payload travels through a JSONB column, and Postgres
     * hands it back normalised - keys reordered, a space after every colon. Only the values are
     * ours to assert on.
     */
    private boolean isTheExpectedInventoryChanged(String value, Sku sku) {
        JsonNode event = objectMapper.readTree(value);
        return sku.value().equals(event.path("sku").path("value").asText())
                && event.path("available").asInt() == 7
                && event.path("previousAvailable").asInt() == 10
                && "ORDER".equals(event.path("reason").asText());
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
}
