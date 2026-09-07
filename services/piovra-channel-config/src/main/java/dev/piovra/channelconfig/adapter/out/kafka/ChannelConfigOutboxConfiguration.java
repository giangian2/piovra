package dev.piovra.channelconfig.adapter.out.kafka;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.outbox.AbstractOutboxConfiguration;
import dev.piovra.outbox.OutboxRelay;
import dev.piovra.outbox.OutboxWriter;

import io.micrometer.core.instrument.MeterRegistry;

@Configuration(proxyBeanMethods = false)
public class ChannelConfigOutboxConfiguration extends AbstractOutboxConfiguration<ChannelConfigOutboxEvent> {

    public ChannelConfigOutboxConfiguration(
            ChannelConfigOutboxRepository repository,
            ObjectMapper objectMapper,
            KafkaTemplate<Object, Object> kafkaTemplate,
            @Value("${piovra.outbox.relay.max-attempts:10}") int maxAttempts,
            ObjectProvider<MeterRegistry> meterRegistry) {
        super(
                repository,
                ChannelConfigOutboxEvent::new,
                objectMapper,
                kafkaTemplate,
                maxAttempts,
                "channel_config",
                meterRegistry.getIfAvailable());
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
