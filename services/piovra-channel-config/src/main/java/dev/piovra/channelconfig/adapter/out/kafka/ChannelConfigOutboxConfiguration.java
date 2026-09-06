package dev.piovra.channelconfig.adapter.out.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.outbox.AbstractOutboxConfiguration;
import dev.piovra.outbox.OutboxRelay;
import dev.piovra.outbox.OutboxWriter;

@Configuration(proxyBeanMethods = false)
public class ChannelConfigOutboxConfiguration extends AbstractOutboxConfiguration<ChannelConfigOutboxEvent> {

    public ChannelConfigOutboxConfiguration(
            ChannelConfigOutboxRepository repository,
            ObjectMapper objectMapper,
            KafkaTemplate<Object, Object> kafkaTemplate) {
        super(repository, ChannelConfigOutboxEvent::new, objectMapper, kafkaTemplate);
    }

    @Bean
    public OutboxWriter channelConfigOutboxWriter() {
        return writer;
    }

    @Bean
    public OutboxRelay<ChannelConfigOutboxEvent> channelConfigOutboxRelay() {
        return relay;
    }
}
