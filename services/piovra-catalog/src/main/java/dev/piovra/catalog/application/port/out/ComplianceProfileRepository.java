package dev.piovra.catalog.application.port.out;

import java.util.List;
import java.util.Optional;

import dev.piovra.common.TenantId;
import dev.piovra.model.compliance.ComplianceProfile;
import dev.piovra.model.compliance.ComplianceProfileType;

public interface ComplianceProfileRepository {

    ComplianceProfile save(ComplianceProfile profile);

    Optional<ComplianceProfile> findById(TenantId tenantId, String id);

    List<ComplianceProfile> findByTenantAndType(TenantId tenantId, ComplianceProfileType type);
}
