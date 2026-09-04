package dev.piovra.outbox;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.kafka.support.KafkaHeaderSupport;

/**
 * Publishes {@code PENDING} outbox rows to Kafka on a fixed schedule.
 *
 * <p>Deliberately a simple poll-and-publish relay rather than CDC (e.g. Debezium): CDC needs Kafka
 * Connect plus {@code wal_level=logical} tuning, real operational machinery that a first working
 * slice does not need. This is a handful of lines and already gives "database write plus Kafka
 * publish in one local transaction, at-least-once" (docs/12-development-guidelines.md section 5.4).
 *
 * <p>{@code tryLock} rather than {@code synchronized}, per docs/12 section 5.3: it avoids two ticks
 * overlapping if a publish run takes longer than the poll interval, without pinning a virtual thread.
 */
public class OutboxRelay<T extends OutboxEntity> {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final TypeReference<Map<String, String>> HEADER_MAP_TYPE = new TypeReference<>() {};

    private final OutboxRepository<T> repository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ReentrantLock lock = new ReentrantLock();

    public OutboxRelay(
            OutboxRepository<T> repository, KafkaTemplate<Object, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${piovra.outbox.relay.poll-interval-ms:500}")
    public void relay() {
        if (!lock.tryLock()) {
            return;
        }
        try {
            List<T> pending = repository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
            pending.forEach(this::publish);
        } finally {
            lock.unlock();
        }
    }

    private void publish(T row) {
        try {
            Map<String, String> headers = objectMapper.readValue(row.headers(), HEADER_MAP_TYPE);
            ProducerRecord<Object, Object> record = new ProducerRecord<>(
                    row.topic(), null, row.partitionKey(), row.payload(), KafkaHeaderSupport.toKafkaHeaders(headers));
            kafkaTemplate.send(record).get();
            row.markPublished();
            repository.save(row);
        } catch (Exception e) {
            log.warn("outbox publish failed, will retry on the next tick: id={} topic={}", row.id(), row.topic(), e);
            row.markFailed(e.getMessage());
            repository.save(row);
        }
    }
}
