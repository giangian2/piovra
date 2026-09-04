package dev.piovra.channelconfig.adapter.out.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.outbox.JpaOutboxWriter;
import dev.piovra.outbox.OutboxRelay;
import dev.piovra.outbox.OutboxWriter;

@Configuration(proxyBeanMethods = false)
public class ChannelConfigOutboxConfiguration {

    @Bean
    public OutboxWriter channelConfigOutboxWriter(ChannelConfigOutboxRepository repository, ObjectMapper objectMapper) {
        return new JpaOutboxWriter<>(repository, ChannelConfigOutboxEvent::new, objectMapper);
    }

    @Bean
    public OutboxRelay<ChannelConfigOutboxEvent> channelConfigOutboxRelay(
            ChannelConfigOutboxRepository repository,
            KafkaTemplate<Object, Object> kafkaTemplate,
            ObjectMapper objectMapper) {
        return new OutboxRelay<>(repository, kafkaTemplate, objectMapper);
    }
}
