package dev.piovra.publication.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.common.Ids;
import dev.piovra.events.ChannelConfigChanged;
import dev.piovra.events.Topics;
import dev.piovra.model.channel.ChannelDefinition;
import dev.piovra.publication.application.port.out.ChannelDefinitionCache;
import dev.piovra.testsupport.ChannelDefinitionFixtures;
import dev.piovra.testsupport.PiovraIntegrationTest;

class ChannelConfigConsumerIT extends PiovraIntegrationTest {

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChannelDefinitionCache channelDefinitionCache;

    @Test
    void a_channel_config_changed_message_updates_the_local_cache() throws Exception {
        ChannelDefinition definition =
                ChannelDefinitionFixtures.channel("cfg-" + Ids.newId().toLowerCase());
        ChannelConfigChanged event = ChannelConfigChanged.of(definition);

        kafkaTemplate
                .send(Topics.CHANNEL_CONFIG, event.channelId().value(), objectMapper.writeValueAsString(event))
                .get();

        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(channelDefinitionCache.activeChannelsFor(definition.tenantId()))
                        .anyMatch(d -> d.channelId().equals(definition.channelId())));
    }
}
