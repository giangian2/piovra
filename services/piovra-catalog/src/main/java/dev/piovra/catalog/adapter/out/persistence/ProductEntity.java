package dev.piovra.catalog.adapter.out.persistence;

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
 * Every query here is a point lookup by {@code (tenant_id, sku)}, never a join across products, so
 * the full {@link dev.piovra.model.product.CanonicalProduct} is kept as one JSONB payload
 * (docs/12-development-guidelines.md - keep it simple; revisit if relational queries are ever
 * needed).
 */
@Entity
@Table(schema = "catalog", name = "products", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "sku"}))
public class ProductEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Column(nullable = false, updatable = false)
    private String sku;

    @Column(nullable = false)
    private long revision;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductEntity() {}

    public ProductEntity(UUID id, String tenantId, String sku, long revision, String status, String payload) {
        this.id = id;
        this.tenantId = tenantId;
        this.sku = sku;
        this.revision = revision;
        this.status = status;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID id() {
        return id;
    }

    public String payload() {
        return payload;
    }

    public void update(long revision, String status, String payload) {
        this.revision = revision;
        this.status = status;
        this.payload = payload;
        this.updatedAt = Instant.now();
    }
}
