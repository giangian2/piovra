package dev.piovra.model.compliance;

import java.time.Instant;
import java.util.Objects;

import dev.piovra.common.TenantId;

/**
 * A GPSR manufacturer or responsible-person contact, reusable across every product of a tenant
 * (docs/adr/0001-canonical-model.md's "expressive lowest common multiple" applies here too: GPSR
 * applies to any EU marketplace, not just one channel). {@link dev.piovra.model.product.CanonicalProduct}
 * references a profile by {@code id} rather than embedding it, so updating a manufacturer's address
 * does not require touching every product that uses it.
 */
public record ComplianceProfile(
        String id,
        TenantId tenantId,
        ComplianceProfileType type,
        String name,
        Address address,
        String email,
        String phone,
        Instant updatedAt) {

    public ComplianceProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(email, "email");
    }
}
