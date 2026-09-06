package dev.piovra.catalog.adapter.out.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.outbox.AbstractOutboxConfiguration;
import dev.piovra.outbox.OutboxRelay;
import dev.piovra.outbox.OutboxWriter;

@Configuration(proxyBeanMethods = false)
public class CatalogOutboxConfiguration extends AbstractOutboxConfiguration<CatalogOutboxEvent> {

    public CatalogOutboxConfiguration(
            CatalogOutboxRepository repository,
            ObjectMapper objectMapper,
            KafkaTemplate<Object, Object> kafkaTemplate) {
        super(repository, CatalogOutboxEvent::new, objectMapper, kafkaTemplate);
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
