package dev.piovra.outbox;

import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.common.ErrorClass;
import dev.piovra.common.PiovraException;
import dev.piovra.events.DomainEvent;
import dev.piovra.kafka.support.KafkaHeaderSupport;

/**
 * Generic {@link OutboxWriter}: serializes the event and the standard headers to JSON, then hands a
 * new row to the module's own repository via a small factory the module provides.
 */
public class JpaOutboxWriter<T extends OutboxEntity> implements OutboxWriter {

    private final OutboxRepository<T> repository;
    private final Function<OutboxRowData, T> factory;
    private final ObjectMapper objectMapper;

    public JpaOutboxWriter(
            OutboxRepository<T> repository, Function<OutboxRowData, T> factory, ObjectMapper objectMapper) {
        this.repository = repository;
        this.factory = factory;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(DomainEvent event) {
        String payload = write(event);
        String headers = write(KafkaHeaderSupport.standardHeaderMap(event));
        OutboxRowData row = new OutboxRowData(
                event.eventId(),
                event.partitionKey(),
                event.topic(),
                event.getClass().getSimpleName(),
                payload,
                headers);
        repository.save(factory.apply(row));
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new PiovraException(ErrorClass.INTERNAL, "OUTBOX_SERIALIZATION", "cannot serialize outbox row", e);
        }
    }

    /** Plain carrier so a module's factory does not need to know {@link OutboxEntity}'s constructor. */
    public record OutboxRowData(
            String id, String partitionKey, String topic, String eventType, String payload, String headers) {}
}
