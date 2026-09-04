package dev.piovra.catalog.adapter;

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

import dev.piovra.catalog.application.port.in.UpsertProductUseCase;
import dev.piovra.common.Ids;
import dev.piovra.common.TenantId;
import dev.piovra.events.Topics;
import dev.piovra.model.product.CanonicalProduct;
import dev.piovra.testsupport.CanonicalProductFixtures;
import dev.piovra.testsupport.PiovraIntegrationTest;
import dev.piovra.testsupport.PiovraKafkaContainer;

/**
 * Proves the whole loop: an upsert produces an outbox row, and the relay publishes it as a
 * {@code ProductChanged} on the real (test) Kafka broker - the exact chain publication will consume.
 */
class CatalogOutboxRelayIT extends PiovraIntegrationTest {

    @Autowired
    private UpsertProductUseCase upsertProductUseCase;

    @Test
    void an_upsert_is_published_as_product_changed() {
        CanonicalProduct product = CanonicalProductFixtures.simpleProduct("TEST-" + Ids.newId());
        upsertProductUseCase.upsert(TenantId.DEFAULT, product);

        try (KafkaConsumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(List.of(Topics.CATALOG_PRODUCT_CHANGED));
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                boolean found = StreamSupport.stream(records.spliterator(), false)
                        .anyMatch(
                                record -> record.value().contains(product.sku().value()));
                assertThat(found).isTrue();
            });
        }
    }

    @Test
    void resubmitting_an_identical_product_publishes_nothing_new() {
        CanonicalProduct product = CanonicalProductFixtures.simpleProduct("TEST-" + Ids.newId());
        upsertProductUseCase.upsert(TenantId.DEFAULT, product);
        upsertProductUseCase.upsert(TenantId.DEFAULT, product);

        try (KafkaConsumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(List.of(Topics.CATALOG_PRODUCT_CHANGED));
            await().pollDelay(Duration.ofSeconds(3))
                    .atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                        long matches = StreamSupport.stream(records.spliterator(), false)
                                .filter(record ->
                                        record.value().contains(product.sku().value()))
                                .count();
                        assertThat(matches).isEqualTo(1);
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
