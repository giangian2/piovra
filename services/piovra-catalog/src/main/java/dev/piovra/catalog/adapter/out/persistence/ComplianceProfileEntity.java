package dev.piovra.catalog.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Flat, queryable columns rather than the JSONB payload {@link ProductEntity} uses: unlike a
 * product, a profile is looked up by id/type across many products, not read by a single point
 * lookup.
 */
@Entity
@Table(schema = "catalog", name = "compliance_profile")
public class ComplianceProfileEntity {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String city;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ComplianceProfileEntity() {}

    public ComplianceProfileEntity(
            String id,
            String tenantId,
            String type,
            String name,
            String street,
            String city,
            String postalCode,
            String countryCode,
            String email,
            String phone,
            Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.type = type;
        this.name = name;
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
        this.email = email;
        this.phone = phone;
        this.updatedAt = updatedAt;
    }

    public String id() {
        return id;
    }

    public String tenantId() {
        return tenantId;
    }

    public String type() {
        return type;
    }

    public String name() {
        return name;
    }

    public String street() {
        return street;
    }

    public String city() {
        return city;
    }

    public String postalCode() {
        return postalCode;
    }

    public String countryCode() {
        return countryCode;
    }

    public String email() {
        return email;
    }

    public String phone() {
        return phone;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
