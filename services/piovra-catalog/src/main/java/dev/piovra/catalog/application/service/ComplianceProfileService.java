package dev.piovra.catalog.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.piovra.catalog.application.port.in.FindComplianceProfileUseCase;
import dev.piovra.catalog.application.port.in.RegisterComplianceProfileUseCase;
import dev.piovra.catalog.application.port.out.ComplianceProfileRepository;
import dev.piovra.common.TenantId;
import dev.piovra.model.compliance.ComplianceProfile;
import dev.piovra.model.compliance.ComplianceProfileType;

/**
 * Plain CRUD over {@link ComplianceProfile}: an admin-managed, low-frequency contact record with no
 * lifecycle events of its own (unlike {@code CanonicalProduct}), so - like {@code
 * ChannelRegistrationService} - there is no diffing here.
 */
@Service
public class ComplianceProfileService implements RegisterComplianceProfileUseCase, FindComplianceProfileUseCase {

    private final ComplianceProfileRepository repository;

    public ComplianceProfileService(ComplianceProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ComplianceProfile register(ComplianceProfile profile) {
        return repository.save(profile);
    }

    @Override
    public Optional<ComplianceProfile> find(TenantId tenantId, String id) {
        return repository.findById(tenantId, id);
    }

    @Override
    public List<ComplianceProfile> findByType(TenantId tenantId, ComplianceProfileType type) {
        return repository.findByTenantAndType(tenantId, type);
    }
}
