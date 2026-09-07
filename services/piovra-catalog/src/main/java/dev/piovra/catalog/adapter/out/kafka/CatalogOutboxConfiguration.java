package dev.piovra.catalog.adapter.out.kafka;

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
public class CatalogOutboxConfiguration extends AbstractOutboxConfiguration<CatalogOutboxEvent> {

    public CatalogOutboxConfiguration(
            CatalogOutboxRepository repository,
            ObjectMapper objectMapper,
            KafkaTemplate<Object, Object> kafkaTemplate,
            @Value("${piovra.outbox.relay.max-attempts:10}") int maxAttempts,
            ObjectProvider<MeterRegistry> meterRegistry) {
        super(
                repository,
                CatalogOutboxEvent::new,
                objectMapper,
                kafkaTemplate,
                maxAttempts,
                "catalog",
                meterRegistry.getIfAvailable());
    }

    @Bean
    public OutboxWriter catalogOutboxWriter() {
        return writer;
    }

    @Bean
    public OutboxRelay<CatalogOutboxEvent> catalogOutboxRelay() {
        return relay;
    }
}
