package dev.piovra.kafka.support;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;

import dev.piovra.events.DomainEvent;
import dev.piovra.events.EventHeaders;

/**
 * Builds the standard Kafka headers (docs/04-kafka-events.md section 2) from a {@link DomainEvent}.
 * One place, so no producer forgets one or spells it differently.
 *
 * <p>{@link #standardHeaderMap} is the form the outbox stores alongside the payload: a plain map
 * survives a JSON column and a Postgres round-trip, unlike {@link Headers}.
 */
public final class KafkaHeaderSupport {

    private KafkaHeaderSupport() {}

    public static Map<String, String> standardHeaderMap(DomainEvent event) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(EventHeaders.EVENT_ID, event.eventId());
        // No ambient correlation id is tracked yet (no request-scoped propagation into Kafka
        // producers in this step): default the correlation id to the event's own id.
        headers.put(EventHeaders.CORRELATION_ID, event.eventId());
        headers.put(EventHeaders.TENANT, event.tenantId().value());
        headers.put(EventHeaders.SCHEMA_VERSION, "1");
        return headers;
    }

    public static Headers standardHeaders(DomainEvent event) {
        return toKafkaHeaders(standardHeaderMap(event));
    }

    public static Headers toKafkaHeaders(Map<String, String> headerMap) {
        RecordHeaders headers = new RecordHeaders();
        headerMap.forEach((key, value) -> headers.add(key, value.getBytes(StandardCharsets.UTF_8)));
        return headers;
    }
}
