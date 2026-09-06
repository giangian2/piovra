package dev.piovra.outbox;

import java.util.function.Function;

import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base for a module's outbox {@code @Configuration}: builds the writer+relay pair once, from a
 * plain constructor rather than generic {@code @Bean} factory-method parameters, so bean
 * resolution stays unambiguous. Subclasses expose {@link #writer} and {@link #relay} through their
 * own named {@code @Bean} getters, so bean identity stays explicit per module - required because
 * every deployable that bundles more than one service (e.g. {@code piovra-core}) ends up with
 * several {@link OutboxWriter} beans in the same context (docs/12-development-guidelines.md
 * section 5.5).
 */
public abstract class AbstractOutboxConfiguration<T extends OutboxEntity> {

    protected final OutboxWriter writer;
    protected final OutboxRelay<T> relay;

    protected AbstractOutboxConfiguration(
            OutboxRepository<T> repository,
            Function<JpaOutboxWriter.OutboxRowData, T> entityFactory,
            ObjectMapper objectMapper,
            KafkaTemplate<Object, Object> kafkaTemplate) {
        this.writer = new JpaOutboxWriter<>(repository, entityFactory, objectMapper);
        this.relay = new OutboxRelay<>(repository, kafkaTemplate, objectMapper);
    }
}
