package dev.piovra.connector.woocommerce.adapter.out.kafka;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import dev.piovra.outbox.AbstractOutboxConfiguration;
import dev.piovra.outbox.OutboxRelay;
import dev.piovra.outbox.OutboxWriter;

import io.micrometer.core.instrument.MeterRegistry;
import tools.jackson.databind.ObjectMapper;

/** Bean names carry the module prefix: every deployable that bundles more than one outbox-writing
 * module ends up with several {@link OutboxWriter} beans in the same context. */
@Configuration(proxyBeanMethods = false)
public class WooOutboxConfiguration extends AbstractOutboxConfiguration<WooOutboxEvent> {

    public WooOutboxConfiguration(
            WooOutboxRepository repository,
            ObjectMapper objectMapper,
            KafkaTemplate<Object, Object> kafkaTemplate,
            @Value("${piovra.outbox.relay.max-attempts:10}") int maxAttempts,
            ObjectProvider<MeterRegistry> meterRegistry) {
        super(
                repository,
                WooOutboxEvent::new,
                objectMapper,
                kafkaTemplate,
                maxAttempts,
                "connector-woocommerce",
                meterRegistry.getIfAvailable());
    }

    @Bean
    public OutboxWriter wooOutboxWriter() {
        return writer;
    }

    @Bean
    public OutboxRelay<WooOutboxEvent> wooOutboxRelay() {
        return relay;
    }
}
