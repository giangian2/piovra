package dev.piovra.inventory.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
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
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.events.InventoryChanged;
import dev.piovra.events.Topics;
import dev.piovra.inventory.application.port.in.ApplyStockMovementsUseCase;
import dev.piovra.inventory.application.port.in.StockMovementCommand;
import dev.piovra.inventory.application.port.in.StockMovementMode;
import dev.piovra.testsupport.PiovraIntegrationTest;
import dev.piovra.testsupport.PiovraKafkaContainer;

/** Proves the whole loop: applying a movement produces an outbox row, and the relay publishes it as
 * {@code InventoryChanged} on the real (test) Kafka broker - mirrors {@code CatalogOutboxRelayIT}. */
class InventoryOutboxRelayIT extends PiovraIntegrationTest {

    @Autowired
    private ApplyStockMovementsUseCase applyStockMovementsUseCase;

    @Test
    void a_movement_that_changes_available_is_published_as_inventory_changed() {
        Sku sku = Sku.of("TEST-" + Ids.newId());
        StockMovementCommand command = new StockMovementCommand(
                sku,
                StockMovementMode.SET,
                5,
                InventoryChanged.Reason.FEED_SET,
                (ChannelId) null,
                "batch-" + Ids.newId());

        applyStockMovementsUseCase.apply(TenantId.DEFAULT, List.of(command));

        try (KafkaConsumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(List.of(Topics.INVENTORY_CHANGED));
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                boolean found = StreamSupport.stream(records.spliterator(), false)
                        .anyMatch(record -> record.value().contains(sku.value()));
                assertThat(found).isTrue();
            });
        }
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
