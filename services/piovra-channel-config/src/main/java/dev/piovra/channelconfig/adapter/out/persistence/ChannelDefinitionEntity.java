package dev.piovra.channelconfig.adapter.out.persistence;

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
 * A single point-lookup table: every query here is by {@code (tenant_id, channel_id)}, never a join,
 * so the full {@link dev.piovra.model.channel.ChannelDefinition} is kept as one JSONB payload rather
 * than a normalized schema.
 */
@Entity
@Table(
        schema = "channel_config",
        name = "channel_definition",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "channel_id"}))
public class ChannelDefinitionEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Column(name = "channel_id", nullable = false, updatable = false)
    private String channelId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ChannelDefinitionEntity() {}

    public ChannelDefinitionEntity(UUID id, String tenantId, String channelId, boolean enabled, String payload) {
        this.id = id;
        this.tenantId = tenantId;
        this.channelId = channelId;
        this.enabled = enabled;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID id() {
        return id;
    }

    public String tenantId() {
        return tenantId;
    }

    public String channelId() {
        return channelId;
    }

    public String payload() {
        return payload;
    }

    public void updatePayload(boolean enabled, String payload) {
        this.enabled = enabled;
        this.payload = payload;
        this.updatedAt = Instant.now();
    }
}
