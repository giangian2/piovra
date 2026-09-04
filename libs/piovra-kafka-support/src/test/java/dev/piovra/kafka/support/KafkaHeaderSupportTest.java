package dev.piovra.kafka.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import org.apache.kafka.common.header.Headers;
import org.junit.jupiter.api.Test;

import dev.piovra.common.TenantId;
import dev.piovra.events.DomainEvent;
import dev.piovra.events.EventHeaders;

class KafkaHeaderSupportTest {

    @Test
    void the_standard_header_map_carries_event_id_tenant_and_schema_version() {
        TestEvent event = new TestEvent("evt-1", TenantId.of("acme"), "piovra.test.v1", "key-1", Instant.EPOCH);

        Map<String, String> headers = KafkaHeaderSupport.standardHeaderMap(event);

        assertThat(headers)
                .containsEntry(EventHeaders.EVENT_ID, "evt-1")
                .containsEntry(EventHeaders.CORRELATION_ID, "evt-1")
                .containsEntry(EventHeaders.TENANT, "acme")
                .containsEntry(EventHeaders.SCHEMA_VERSION, "1");
    }

    @Test
    void kafka_headers_carry_the_same_values_as_the_map() {
        Headers headers = KafkaHeaderSupport.toKafkaHeaders(Map.of("a", "1", "b", "2"));

        assertThat(new String(headers.lastHeader("a").value(), StandardCharsets.UTF_8))
                .isEqualTo("1");
        assertThat(new String(headers.lastHeader("b").value(), StandardCharsets.UTF_8))
                .isEqualTo("2");
    }

    @Test
    void standard_headers_is_standard_header_map_converted_to_kafka_headers() {
        TestEvent event = new TestEvent("evt-2", TenantId.of("acme"), "piovra.test.v1", "key-2", Instant.EPOCH);

        Headers headers = KafkaHeaderSupport.standardHeaders(event);

        assertThat(new String(headers.lastHeader(EventHeaders.EVENT_ID).value(), StandardCharsets.UTF_8))
                .isEqualTo("evt-2");
    }

    private record TestEvent(String eventId, TenantId tenantId, String topic, String partitionKey, Instant occurredAt)
            implements DomainEvent {}
}
