package dev.piovra.order.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.Money;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.events.Topics;
import dev.piovra.model.order.Address;
import dev.piovra.model.order.Buyer;
import dev.piovra.model.order.CanonicalOrder;
import dev.piovra.model.order.LineResolution;
import dev.piovra.model.order.OrderLine;
import dev.piovra.model.order.OrderStatus;
import dev.piovra.model.order.OrderTotals;
import dev.piovra.order.application.port.in.IngestOrderUseCase;
import dev.piovra.order.application.port.out.KnownSkuRepository;
import dev.piovra.testsupport.PiovraIntegrationTest;
import dev.piovra.testsupport.PiovraKafkaContainer;

/** Proves the whole loop: an accepted order produces an outbox row, and the relay publishes it as
 * {@code OrderAccepted} on the real (test) Kafka broker - mirrors {@code CatalogOutboxRelayIT}. */
class OrderOutboxRelayIT extends PiovraIntegrationTest {

    private static final TenantId TENANT = TenantId.DEFAULT;
    private static final ChannelId CHANNEL = ChannelId.of("test-channel");

    @Autowired
    private IngestOrderUseCase ingestOrderUseCase;

    @Autowired
    private KnownSkuRepository knownSkuRepository;

    @Test
    void an_ingested_order_is_published_as_order_accepted() {
        Sku sku = Sku.of("TEST-" + Ids.newId());
        knownSkuRepository.ensureExists(TENANT, sku);

        ingestOrderUseCase.ingest(order(sku), null);

        try (KafkaConsumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(List.of(Topics.ORDER_ACCEPTED));
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                boolean found = StreamSupport.stream(records.spliterator(), false)
                        .anyMatch(record -> record.value().contains(sku.value()));
                assertThat(found).isTrue();
            });
        }
    }

    private static CanonicalOrder order(Sku sku) {
        OrderLine line = new OrderLine(
                "line-1", "channel-line-1", sku.value(), null, LineResolution.UNMAPPED, 1, Money.euro("19.90"));
        return new CanonicalOrder(
                Ids.newId(),
                TENANT,
                CHANNEL,
                "CH-" + Ids.newId(),
                OrderStatus.NEW,
                "processing",
                Instant.now(),
                Instant.now(),
                new Buyer("buyer-1", "Mario Rossi", "mario@test.it"),
                new Address("Mario Rossi", "Via Roma 1", null, "Milano", "MI", "20100", "IT", null),
                new OrderTotals(Money.euro("19.90"), Money.euro("0.00"), Money.euro("0.00"), Money.euro("19.90")),
                List.of(line),
                false);
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
