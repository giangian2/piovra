package dev.piovra.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
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
 *
 * <p>A failure schedules an exponential-backoff retry ({@link BackoffCalculator}) rather than being
 * retried on every single tick forever; after {@code maxAttempts} the row is dead-lettered
 * ({@link OutboxStatus#FAILED}) and stops being fetched - inspectable via SQL, no admin tooling yet.
 */
public class OutboxRelay<T extends OutboxEntity> {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final TypeReference<Map<String, String>> HEADER_MAP_TYPE = new TypeReference<>() {};
    private static final Pageable TOP_100 = Pageable.ofSize(100);

    private final OutboxRepository<T> repository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final int maxAttempts;
    private final BackoffCalculator backoffCalculator;
    private final OutboxMetrics metrics;
    private final ReentrantLock lock = new ReentrantLock();

    public OutboxRelay(
            OutboxRepository<T> repository,
            KafkaTemplate<Object, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            int maxAttempts,
            OutboxMetrics metrics) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.maxAttempts = maxAttempts;
        this.backoffCalculator = new BackoffCalculator();
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${piovra.outbox.relay.poll-interval-ms:500}")
    public void relay() {
        if (!lock.tryLock()) {
            return;
        }
        try {
            List<T> pending = repository.findReadyToPublish(OutboxStatus.PENDING, Instant.now(), TOP_100);
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
            row.markFailed(e.getMessage());
            if (row.attempts() >= maxAttempts) {
                row.markPermanentlyFailed();
                metrics.permanentFailure();
                log.error(
                        "outbox row dead-lettered after {} attempts: id={} topic={}",
                        row.attempts(),
                        row.id(),
                        row.topic(),
                        e);
            } else {
                row.scheduleRetry(Instant.now().plus(backoffCalculator.compute(row.attempts())));
                log.warn(
                        "outbox publish failed, will retry with backoff: id={} topic={} attempt={}",
                        row.id(),
                        row.topic(),
                        row.attempts(),
                        e);
            }
            repository.save(row);
        }
    }
}
