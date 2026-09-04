package dev.piovra.catalog.adapter.out.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.outbox.JpaOutboxWriter;
import dev.piovra.outbox.OutboxRelay;
import dev.piovra.outbox.OutboxWriter;

@Configuration(proxyBeanMethods = false)
public class CatalogOutboxConfiguration {

    @Bean
    public OutboxWriter catalogOutboxWriter(CatalogOutboxRepository repository, ObjectMapper objectMapper) {
        return new JpaOutboxWriter<>(repository, CatalogOutboxEvent::new, objectMapper);
    }

    @Bean
    public OutboxRelay<CatalogOutboxEvent> catalogOutboxRelay(
            CatalogOutboxRepository repository,
            KafkaTemplate<Object, Object> kafkaTemplate,
            ObjectMapper objectMapper) {
        return new OutboxRelay<>(repository, kafkaTemplate, objectMapper);
    }
}
