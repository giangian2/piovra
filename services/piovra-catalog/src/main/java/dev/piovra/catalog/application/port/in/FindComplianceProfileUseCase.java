package dev.piovra.catalog.application.port.in;

import java.util.List;
import java.util.Optional;

import dev.piovra.common.TenantId;
import dev.piovra.model.compliance.ComplianceProfile;
import dev.piovra.model.compliance.ComplianceProfileType;

public interface FindComplianceProfileUseCase {

    Optional<ComplianceProfile> find(TenantId tenantId, String id);

    List<ComplianceProfile> findByType(TenantId tenantId, ComplianceProfileType type);
}
