package dev.piovra.outbox;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Base row of a transactional outbox: a domain event waiting to be published to Kafka in the same
 * local transaction as the business write (docs/12-development-guidelines.md section 5.4 - "database
 * write plus Kafka publish: always the outbox").
 *
 * <p>Deliberately a {@code @MappedSuperclass}, not a concrete {@code @Entity}: every module that
 * writes to the outbox declares its own trivial subclass and its own {@code outbox_event} table in
 * its own schema, so "each module touches only its own schema" stays true at the Postgres grant
 * level (docs/12-development-guidelines.md section 5.5), instead of one shared table crossing every
 * service boundary.
 */
@MappedSuperclass
public abstract class OutboxEntity {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false, updatable = false)
    private String partitionKey;

    @Column(nullable = false, updatable = false)
    private String topic;

    @Column(nullable = false, updatable = false)
    private String eventType;

    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    /** Kafka headers to replay verbatim at publish time, computed once at write time. */
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String headers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    private String lastError;

    protected OutboxEntity() {}

    protected OutboxEntity(
            String id, String partitionKey, String topic, String eventType, String payload, String headers) {
        this.id = id;
        this.partitionKey = partitionKey;
        this.topic = topic;
        this.eventType = eventType;
        this.payload = payload;
        this.headers = headers;
        this.createdAt = Instant.now();
    }

    public String id() {
        return id;
    }

    public String partitionKey() {
        return partitionKey;
    }

    public String topic() {
        return topic;
    }

    public String eventType() {
        return eventType;
    }

    public String payload() {
        return payload;
    }

    public String headers() {
        return headers;
    }

    public OutboxStatus status() {
        return status;
    }

    public int attempts() {
        return attempts;
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.attempts++;
        this.lastError = error;
    }
}
