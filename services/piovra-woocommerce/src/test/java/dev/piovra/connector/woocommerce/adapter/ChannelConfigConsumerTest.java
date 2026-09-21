package dev.piovra.connector.woocommerce.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import dev.piovra.common.Ids;
import dev.piovra.events.ChannelConfigChanged;
import dev.piovra.events.Topics;
import dev.piovra.model.channel.ChannelDefinition;
import dev.piovra.model.channel.ChannelType;
import dev.piovra.testsupport.ChannelDefinitionFixtures;
import dev.piovra.testsupport.PiovraIntegrationTest;

import tools.jackson.databind.ObjectMapper;

/**
 * The connector learns which channels exist by replaying the compacted {@code channel.config.v1}
 * topic, never by calling channel-config ({@code ArchitectureTest.services_do_not_call_each_other}).
 */
class ChannelConfigConsumerTest extends PiovraIntegrationTest {

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void a_woocommerce_channel_is_cached_and_given_a_polling_cursor() throws Exception {
        ChannelDefinition channel = woo();

        send(channel);

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(cursorRows(channel)).isEqualTo(1));
        assertThat(cacheRows(channel)).isEqualTo(1);
    }

    @Test
    void a_channel_of_another_type_is_cached_but_never_polled() throws Exception {
        ChannelDefinition ebay =
                ChannelDefinitionFixtures.channel("ebay-" + Ids.newId().toLowerCase(), ChannelType.EBAY);

        send(ebay);

        // Every connector replays the whole topic; only its own type gets a cursor.
        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(cacheRows(ebay)).isEqualTo(1));
        assertThat(cursorRows(ebay)).isZero();
    }

    @Test
    void the_same_channel_config_event_twice_leaves_one_row() throws Exception {
        ChannelDefinition channel = woo();

        send(channel);
        send(channel);

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(cursorRows(channel)).isEqualTo(1));
        // An upsert by key on a compacted topic is idempotent by construction: no @Idempotent needed.
        assertThat(cacheRows(channel)).isEqualTo(1);
    }

    private static ChannelDefinition woo() {
        return ChannelDefinitionFixtures.channel("woo-" + Ids.newId().toLowerCase(), ChannelType.WOOCOMMERCE);
    }

    private void send(ChannelDefinition channel) throws Exception {
        ChannelConfigChanged event = ChannelConfigChanged.of(channel);
        kafkaTemplate
                .send(Topics.CHANNEL_CONFIG, event.partitionKey(), objectMapper.writeValueAsString(event))
                .get();
    }

    private int cursorRows(ChannelDefinition channel) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM connector_woocommerce.poll_cursor WHERE channel_id = ?",
                Integer.class,
                channel.channelId().value());
    }

    private int cacheRows(ChannelDefinition channel) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM connector_woocommerce.channel_definition_cache WHERE channel_id = ?",
                Integer.class,
                channel.channelId().value());
    }
}
