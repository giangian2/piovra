package dev.piovra.catalog.application.port.in;

import dev.piovra.model.compliance.ComplianceProfile;

public interface RegisterComplianceProfileUseCase {

    ComplianceProfile register(ComplianceProfile profile);
}
