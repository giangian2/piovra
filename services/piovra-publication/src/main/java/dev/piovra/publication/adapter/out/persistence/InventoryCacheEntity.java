package dev.piovra.publication.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Publication's local read-model of available stock, kept in sync by consuming
 * {@code inventory.changed.v1} (see {@code InventoryCache}). Just an int, no JSONB needed. */
@Entity
@Table(
        schema = "publication",
        name = "inventory_cache",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "sku"}))
public class InventoryCacheEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Column(nullable = false, updatable = false)
    private String sku;

    @Column(nullable = false)
    private int available;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryCacheEntity() {}

    public InventoryCacheEntity(UUID id, String tenantId, String sku, int available) {
        this.id = id;
        this.tenantId = tenantId;
        this.sku = sku;
        this.available = available;
        this.updatedAt = Instant.now();
    }

    public String tenantId() {
        return tenantId;
    }

    public String sku() {
        return sku;
    }

    public int available() {
        return available;
    }

    public void update(int available) {
        this.available = available;
        this.updatedAt = Instant.now();
    }
}
