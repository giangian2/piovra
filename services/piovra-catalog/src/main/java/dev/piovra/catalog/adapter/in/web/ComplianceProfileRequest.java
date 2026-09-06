package dev.piovra.catalog.adapter.in.web;

import java.time.Instant;

import dev.piovra.common.Ids;
import dev.piovra.common.TenantId;
import dev.piovra.model.compliance.Address;
import dev.piovra.model.compliance.ComplianceProfile;
import dev.piovra.model.compliance.ComplianceProfileType;

/** Request body of {@code POST /v1/compliance-profiles}: unlike products (keyed by sku) or channels
 * (keyed by channelId), the id is a server-generated technical identifier, not a natural key from the
 * caller. */
public record ComplianceProfileRequest(
        ComplianceProfileType type, String name, Address address, String email, String phone) {

    public ComplianceProfile toProfile(TenantId tenantId) {
        return new ComplianceProfile(Ids.newId(), tenantId, type, name, address, email, phone, Instant.now());
    }
}
