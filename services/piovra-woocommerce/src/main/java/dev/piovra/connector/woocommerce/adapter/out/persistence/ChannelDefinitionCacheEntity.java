package dev.piovra.connector.woocommerce.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A JSONB mirror of {@link dev.piovra.model.channel.ChannelDefinition}, kept in sync by consuming
 * {@code channel.config.v1}. The type is denormalized into its own column so the poller can ask for
 * "every enabled WooCommerce channel" without deserializing the whole cache. */
@Entity
@Table(
        schema = "connector_woocommerce",
        name = "channel_definition_cache",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "channel_id"}))
public class ChannelDefinitionCacheEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Column(name = "channel_id", nullable = false, updatable = false)
    private String channelId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ChannelDefinitionCacheEntity() {}

    public ChannelDefinitionCacheEntity(
            UUID id, String tenantId, String channelId, String type, boolean enabled, String payload) {
        this.id = id;
        this.tenantId = tenantId;
        this.channelId = channelId;
        this.type = type;
        this.enabled = enabled;
        this.payload = payload;
        this.updatedAt = Instant.now();
    }

    public String payload() {
        return payload;
    }

    public void update(String type, boolean enabled, String payload) {
        this.type = type;
        this.enabled = enabled;
        this.payload = payload;
        this.updatedAt = Instant.now();
    }
}
