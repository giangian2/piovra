package dev.piovra.inventory.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Flat columns, not JSONB like {@code ProductEntity}: a stock level is mutated numerically under a
 * row lock ({@code on_hand} +/- a delta), never replaced wholesale, so there is no single blob to
 * swap - the opposite reasoning from a product, the same "flat because it is not a point-read replace"
 * conclusion as {@code ComplianceProfileEntity}.
 */
@Entity
@Table(
        schema = "inventory",
        name = "stock_level",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "sku"}))
public class StockLevelEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Column(nullable = false, updatable = false)
    private String sku;

    @Column(name = "on_hand", nullable = false)
    private int onHand;

    @Column(nullable = false)
    private int reserved;

    @Column(nullable = false)
    private int buffer;

    @Column(nullable = false)
    private int available;

    @Column(nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StockLevelEntity() {}

    public StockLevelEntity(
            UUID id, String tenantId, String sku, int onHand, int reserved, int buffer, int available, long version) {
        this.id = id;
        this.tenantId = tenantId;
        this.sku = sku;
        this.onHand = onHand;
        this.reserved = reserved;
        this.buffer = buffer;
        this.available = available;
        this.version = version;
        this.updatedAt = Instant.now();
    }

    public String tenantId() {
        return tenantId;
    }

    public String sku() {
        return sku;
    }

    public int onHand() {
        return onHand;
    }

    public int reserved() {
        return reserved;
    }

    public int buffer() {
        return buffer;
    }

    public int available() {
        return available;
    }

    public long version() {
        return version;
    }

    public void update(int onHand, int reserved, int buffer, int available, long version) {
        this.onHand = onHand;
        this.reserved = reserved;
        this.buffer = buffer;
        this.available = available;
        this.version = version;
        this.updatedAt = Instant.now();
    }
}
