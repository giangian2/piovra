package dev.piovra.kafka.support;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;

import dev.piovra.crosscutting.MdcKeys;
import dev.piovra.events.EventHeaders;

/**
 * Populates the MDC from the standard headers before a listener runs, and clears it afterwards. The
 * Kafka-side equivalent of an HTTP correlation filter, so every consumer's logs are correlatable for
 * free (docs/09-errors-observability.md section 6.3).
 */
public class MdcRecordInterceptor implements RecordInterceptor<Object, Object> {

    @Override
    public ConsumerRecord<Object, Object> intercept(
            ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        header(record, EventHeaders.CORRELATION_ID).ifPresent(v -> MDC.put(MdcKeys.CORRELATION_ID, v));
        header(record, EventHeaders.TENANT).ifPresent(v -> MDC.put(MdcKeys.TENANT, v));
        header(record, EventHeaders.CHANNEL_ID).ifPresent(v -> MDC.put(MdcKeys.CHANNEL, v));
        return record;
    }

    @Override
    public void success(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        clear();
    }

    @Override
    public void failure(ConsumerRecord<Object, Object> record, Exception exception, Consumer<Object, Object> consumer) {
        clear();
    }

    private void clear() {
        MDC.remove(MdcKeys.CORRELATION_ID);
        MDC.remove(MdcKeys.TENANT);
        MDC.remove(MdcKeys.CHANNEL);
    }

    private static Optional<String> header(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? Optional.empty() : Optional.of(new String(header.value(), StandardCharsets.UTF_8));
    }
}
