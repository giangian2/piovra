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
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.Money;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.events.OrderReceived;
import dev.piovra.events.Topics;
import dev.piovra.model.order.Address;
import dev.piovra.model.order.Buyer;
import dev.piovra.model.order.CanonicalOrder;
import dev.piovra.model.order.LineResolution;
import dev.piovra.model.order.OrderLine;
import dev.piovra.model.order.OrderStatus;
import dev.piovra.model.order.OrderTotals;
import dev.piovra.order.application.port.out.KnownSkuRepository;
import dev.piovra.testsupport.PiovraIntegrationTest;
import dev.piovra.testsupport.PiovraKafkaContainer;

class ChannelOrderReceivedConsumerIT extends PiovraIntegrationTest {

    private static final TenantId TENANT = TenantId.DEFAULT;
    private static final ChannelId CHANNEL = ChannelId.of("test-channel");

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KnownSkuRepository knownSkuRepository;

    @Test
    void a_new_order_with_a_known_sku_is_accepted() throws Exception {
        Sku sku = Sku.of("TEST-" + Ids.newId());
        knownSkuRepository.ensureExists(TENANT, sku);
        String channelOrderId = "CH-" + Ids.newId();

        send(rawOrder(channelOrderId, sku, OrderStatus.NEW));

        try (KafkaConsumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(List.of(Topics.ORDER_ACCEPTED));
            await().atMost(Duration.ofSeconds(15))
                    .untilAsserted(() -> assertThat(polled(consumer)).anyMatch(v -> v.contains(sku.value())));
        }
    }

    @Test
    void resending_the_same_order_with_the_same_status_produces_no_new_event() throws Exception {
        Sku sku = Sku.of("TEST-" + Ids.newId());
        knownSkuRepository.ensureExists(TENANT, sku);
        String channelOrderId = "CH-" + Ids.newId();
        CanonicalOrder raw = rawOrder(channelOrderId, sku, OrderStatus.NEW);

        send(raw);
        send(raw);

        try (KafkaConsumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(List.of(Topics.ORDER_ACCEPTED));
            await().pollDelay(Duration.ofSeconds(3))
                    .atMost(Duration.ofSeconds(15))
                    .untilAsserted(() -> {
                        long matches = polled(consumer).stream()
                                .filter(v -> v.contains(sku.value()))
                                .count();
                        assertThat(matches).isEqualTo(1);
                    });
        }
    }

    @Test
    void a_status_change_to_cancelled_emits_status_changed_and_a_restoring_accepted() throws Exception {
        Sku sku = Sku.of("TEST-" + Ids.newId());
        knownSkuRepository.ensureExists(TENANT, sku);
        String channelOrderId = "CH-" + Ids.newId();

        // Both messages share the same partition key (tenant|channel|channelOrderId) and each
        // send(...).get() blocks for the broker ack before the next is produced, so Kafka guarantees
        // NEW is consumed before CANCELLED - no artificial wait needed between them.
        send(rawOrder(channelOrderId, sku, OrderStatus.NEW));
        send(rawOrder(channelOrderId, sku, OrderStatus.CANCELLED));

        try (KafkaConsumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(List.of(Topics.ORDER_STATUS_CHANGED));
            await().atMost(Duration.ofSeconds(15))
                    .untilAsserted(() -> assertThat(polled(consumer)).anyMatch(v -> v.contains("CANCELLED")));
        }
        try (KafkaConsumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(List.of(Topics.ORDER_ACCEPTED));
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                long matches = polled(consumer).stream()
                        .filter(v -> v.contains(sku.value()))
                        .count();
                assertThat(matches).isEqualTo(2);
            });
        }
    }

    private void send(CanonicalOrder order) throws Exception {
        OrderReceived event = OrderReceived.of(order, null);
        kafkaTemplate
                .send(Topics.CHANNEL_ORDER_RECEIVED, event.partitionKey(), objectMapper.writeValueAsString(event))
                .get();
    }

    private static CanonicalOrder rawOrder(String channelOrderId, Sku sku, OrderStatus status) {
        OrderLine line = new OrderLine(
                "line-1", "channel-line-1", sku.value(), null, LineResolution.UNMAPPED, 1, Money.euro("19.90"));
        return new CanonicalOrder(
                Ids.newId(),
                TENANT,
                CHANNEL,
                channelOrderId,
                status,
                status.name(),
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.now(),
                new Buyer("buyer-1", "Mario Rossi", "mario@test.it"),
                new Address("Mario Rossi", "Via Roma 1", null, "Milano", "MI", "20100", "IT", null),
                new OrderTotals(Money.euro("19.90"), Money.euro("0.00"), Money.euro("0.00"), Money.euro("19.90")),
                List.of(line),
                false);
    }

    private static List<String> polled(KafkaConsumer<String, String> consumer) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
        return StreamSupport.stream(records.spliterator(), false)
                .map(r -> r.value())
                .toList();
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
