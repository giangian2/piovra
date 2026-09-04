package dev.piovra.channelconfig.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.piovra.channelconfig.application.port.in.RegisterChannelUseCase;
import dev.piovra.common.Ids;
import dev.piovra.events.Topics;
import dev.piovra.model.channel.ChannelDefinition;
import dev.piovra.testsupport.ChannelDefinitionFixtures;
import dev.piovra.testsupport.PiovraIntegrationTest;
import dev.piovra.testsupport.PiovraKafkaContainer;

/**
 * Proves the outbox actually relays: a registration produces a row in {@code outbox_event}, and the
 * relay's scheduled tick publishes it to the real (test) Kafka broker.
 */
class ChannelConfigOutboxRelayIT extends PiovraIntegrationTest {

    @Autowired
    private RegisterChannelUseCase registerChannelUseCase;

    @Test
    void a_registration_is_published_on_the_channel_config_topic() {
        ChannelDefinition definition =
                ChannelDefinitionFixtures.channel("relay-" + Ids.newId().toLowerCase());
        registerChannelUseCase.register(definition);

        try (KafkaConsumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(java.util.List.of(Topics.CHANNEL_CONFIG));
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                boolean found = StreamSupport.stream(records.spliterator(), false)
                        .anyMatch(record ->
                                record.key().equals(definition.channelId().value()));
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
