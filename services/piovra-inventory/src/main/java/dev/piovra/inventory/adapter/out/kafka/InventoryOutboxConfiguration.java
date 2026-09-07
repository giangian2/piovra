package dev.piovra.inventory.adapter.out.kafka;

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
public class InventoryOutboxConfiguration extends AbstractOutboxConfiguration<InventoryOutboxEvent> {

    public InventoryOutboxConfiguration(
            InventoryOutboxRepository repository,
            ObjectMapper objectMapper,
            KafkaTemplate<Object, Object> kafkaTemplate,
            @Value("${piovra.outbox.relay.max-attempts:10}") int maxAttempts,
            ObjectProvider<MeterRegistry> meterRegistry) {
        super(
                repository,
                InventoryOutboxEvent::new,
                objectMapper,
                kafkaTemplate,
                maxAttempts,
                "inventory",
                meterRegistry.getIfAvailable());
    }

    @Bean
    public OutboxWriter inventoryOutboxWriter() {
        return writer;
    }

    @Bean
    public OutboxRelay<InventoryOutboxEvent> inventoryOutboxRelay() {
        return relay;
    }
}
