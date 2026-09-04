package dev.piovra.publication.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.common.Ids;
import dev.piovra.events.CommandPriority;
import dev.piovra.events.ProductChanged;
import dev.piovra.events.Topics;
import dev.piovra.model.channel.ChannelDefinition;
import dev.piovra.model.channel.ChannelType;
import dev.piovra.model.product.CanonicalProduct;
import dev.piovra.publication.application.port.out.ChannelDefinitionCache;
import dev.piovra.publication.application.port.out.ChannelListingRepository;
import dev.piovra.publication.domain.ChannelListing;
import dev.piovra.publication.domain.ListingState;
import dev.piovra.testsupport.CanonicalProductFixtures;
import dev.piovra.testsupport.ChannelDefinitionFixtures;
import dev.piovra.testsupport.PiovraIntegrationTest;
import dev.piovra.testsupport.PiovraKafkaContainer;

/**
 * The full catalog -&gt; publication loop: a {@code ProductChanged} produces a {@code PENDING}
 * listing and a {@code ChannelCommand}, reusing the existing {@code ChannelProjector}/
 * {@code DiffCalculator} domain untouched. The second test is docs/12-development-guidelines.md
 * section 6's mandatory case: receiving the same event twice must not double-publish.
 */
class ProductChangedConsumerIT extends PiovraIntegrationTest {

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChannelDefinitionCache channelDefinitionCache;

    @Autowired
    private ChannelListingRepository channelListingRepository;

    @Test
    void a_product_changed_message_produces_a_pending_listing_and_a_channel_command() throws Exception {
        ChannelDefinition channel =
                ChannelDefinitionFixtures.channel("prod-" + Ids.newId().toLowerCase(), ChannelType.WOOCOMMERCE);
        channelDefinitionCache.upsert(channel);

        CanonicalProduct product = CanonicalProductFixtures.simpleProduct("TEST-" + Ids.newId());
        send(ProductChanged.created(product));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<ChannelListing> listing =
                    channelListingRepository.find(product.tenantId(), product.sku(), channel.channelId());
            assertThat(listing).isPresent();
            assertThat(listing.orElseThrow().state()).isEqualTo(ListingState.PENDING);
        });

        String commandTopic = Topics.channelCommand(channel.type(), CommandPriority.NORMAL);
        try (KafkaConsumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(List.of(commandTopic));
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
    void redelivering_the_same_event_does_not_emit_a_second_command() throws Exception {
        ChannelDefinition channel =
                ChannelDefinitionFixtures.channel("dup-" + Ids.newId().toLowerCase(), ChannelType.WOOCOMMERCE);
        channelDefinitionCache.upsert(channel);

        CanonicalProduct product = CanonicalProductFixtures.simpleProduct("TEST-" + Ids.newId());
        ProductChanged event = ProductChanged.created(product);

        // Kafka's normal mode of operation is at-least-once: the same message can arrive twice.
        send(event);
        send(event);

        String commandTopic = Topics.channelCommand(channel.type(), CommandPriority.NORMAL);
        try (KafkaConsumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(List.of(commandTopic));
            await().pollDelay(Duration.ofSeconds(5))
                    .atMost(Duration.ofSeconds(20))
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

    private void send(ProductChanged event) throws Exception {
        kafkaTemplate
                .send(Topics.CATALOG_PRODUCT_CHANGED, event.partitionKey(), objectMapper.writeValueAsString(event))
                .get();
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
