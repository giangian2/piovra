package dev.piovra.order.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Flat columns for the dedup lookup - {@code (tenant_id, channel_id, channel_order_id)} is the
 * natural key (docs/07-order-flow.md section 3, {@code CanonicalOrder}'s own javadoc) - the rest
 * (buyer, address, totals, lines, timestamps) as one JSONB payload, same reasoning as {@code
 * ProductEntity}. {@code id} is the order's own ULID ({@code CanonicalOrder.orderId}), not a
 * separate surrogate UUID.
 */
@Entity
@Table(
        schema = "orders",
        name = "orders",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "channel_id", "channel_order_id"}))
public class OrderEntity {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Column(name = "channel_id", nullable = false, updatable = false)
    private String channelId;

    @Column(name = "channel_order_id", nullable = false, updatable = false)
    private String channelOrderId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderEntity() {}

    public OrderEntity(
            String id, String tenantId, String channelId, String channelOrderId, String status, String payload) {
        this.id = id;
        this.tenantId = tenantId;
        this.channelId = channelId;
        this.channelOrderId = channelOrderId;
        this.status = status;
        this.payload = payload;
        this.updatedAt = Instant.now();
    }

    public String id() {
        return id;
    }

    public String payload() {
        return payload;
    }

    public void update(String status, String payload) {
        this.status = status;
        this.payload = payload;
        this.updatedAt = Instant.now();
    }
}
