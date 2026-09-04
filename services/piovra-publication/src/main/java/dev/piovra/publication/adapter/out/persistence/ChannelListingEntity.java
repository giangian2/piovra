package dev.piovra.publication.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Mirrors docs/03-data-model.md section 3 almost verbatim, including columns the current
 * {@link dev.piovra.publication.domain.ChannelListing} does not populate yet
 * ({@code published_snapshot}, {@code snapshot_hash}, {@code next_retry_at}): cheap to provision now
 * so reconciliation and retry scheduling do not need a migration later
 * (docs/12-development-guidelines.md section 5.5, expand/contract).
 */
@Entity
@Table(
        schema = "publication",
        name = "channel_listing",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "sku", "channel_id"}))
public class ChannelListingEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Column(nullable = false, updatable = false)
    private String sku;

    @Column(name = "channel_id", nullable = false, updatable = false)
    private String channelId;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "external_variant_ids", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String externalVariantIds;

    @Column(nullable = false)
    private String state;

    @Column(name = "published_revision", nullable = false)
    private long publishedRevision;

    @Column(name = "field_hashes", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String fieldHashes;

    @Column(name = "last_command_id")
    private String lastCommandId;

    @Column(name = "last_error_code")
    private String lastErrorCode;

    @Column(name = "last_error_message")
    private String lastErrorMessage;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    protected ChannelListingEntity() {}

    public ChannelListingEntity(
            UUID id,
            String tenantId,
            String sku,
            String channelId,
            String externalId,
            String externalVariantIds,
            String state,
            long publishedRevision,
            String fieldHashes,
            String lastCommandId,
            String lastErrorCode,
            String lastErrorMessage,
            Instant lastAttemptAt,
            Instant lastSuccessAt,
            int retryCount) {
        this.id = id;
        this.tenantId = tenantId;
        this.sku = sku;
        this.channelId = channelId;
        apply(
                externalId,
                externalVariantIds,
                state,
                publishedRevision,
                fieldHashes,
                lastCommandId,
                lastErrorCode,
                lastErrorMessage,
                lastAttemptAt,
                lastSuccessAt,
                retryCount);
    }

    public void apply(
            String externalId,
            String externalVariantIds,
            String state,
            long publishedRevision,
            String fieldHashes,
            String lastCommandId,
            String lastErrorCode,
            String lastErrorMessage,
            Instant lastAttemptAt,
            Instant lastSuccessAt,
            int retryCount) {
        this.externalId = externalId;
        this.externalVariantIds = externalVariantIds;
        this.state = state;
        this.publishedRevision = publishedRevision;
        this.fieldHashes = fieldHashes;
        this.lastCommandId = lastCommandId;
        this.lastErrorCode = lastErrorCode;
        this.lastErrorMessage = lastErrorMessage;
        this.lastAttemptAt = lastAttemptAt;
        this.lastSuccessAt = lastSuccessAt;
        this.retryCount = retryCount;
    }

    public String tenantId() {
        return tenantId;
    }

    public String sku() {
        return sku;
    }

    public String channelId() {
        return channelId;
    }

    public String externalId() {
        return externalId;
    }

    public String externalVariantIds() {
        return externalVariantIds;
    }

    public String state() {
        return state;
    }

    public long publishedRevision() {
        return publishedRevision;
    }

    public String fieldHashes() {
        return fieldHashes;
    }

    public String lastCommandId() {
        return lastCommandId;
    }

    public String lastErrorCode() {
        return lastErrorCode;
    }

    public String lastErrorMessage() {
        return lastErrorMessage;
    }

    public Instant lastAttemptAt() {
        return lastAttemptAt;
    }

    public Instant lastSuccessAt() {
        return lastSuccessAt;
    }

    public int retryCount() {
        return retryCount;
    }
}
