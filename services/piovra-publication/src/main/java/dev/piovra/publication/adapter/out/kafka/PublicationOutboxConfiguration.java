package dev.piovra.publication.adapter.out.kafka;

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
public class PublicationOutboxConfiguration extends AbstractOutboxConfiguration<PublicationOutboxEvent> {

    public PublicationOutboxConfiguration(
            PublicationOutboxRepository repository,
            ObjectMapper objectMapper,
            KafkaTemplate<Object, Object> kafkaTemplate,
            @Value("${piovra.outbox.relay.max-attempts:10}") int maxAttempts,
            ObjectProvider<MeterRegistry> meterRegistry) {
        super(
                repository,
                PublicationOutboxEvent::new,
                objectMapper,
                kafkaTemplate,
                maxAttempts,
                "publication",
                meterRegistry.getIfAvailable());
    }

    @Bean
    public OutboxWriter publicationOutboxWriter() {
        return writer;
    }

    @Bean
    public OutboxRelay<PublicationOutboxEvent> publicationOutboxRelay() {
        return relay;
    }
}
