package dev.piovra.catalog.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import dev.piovra.catalog.application.port.out.ComplianceProfileRepository;
import dev.piovra.common.TenantId;
import dev.piovra.model.compliance.ComplianceProfile;
import dev.piovra.model.compliance.ComplianceProfileType;

@Repository
public class ComplianceProfileRepositoryAdapter implements ComplianceProfileRepository {

    private final ComplianceProfileJpaRepository jpaRepository;
    private final ComplianceProfileEntityMapper mapper;

    public ComplianceProfileRepositoryAdapter(
            ComplianceProfileJpaRepository jpaRepository, ComplianceProfileEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ComplianceProfile save(ComplianceProfile profile) {
        jpaRepository.save(mapper.toEntity(profile));
        return profile;
    }

    @Override
    public Optional<ComplianceProfile> findById(TenantId tenantId, String id) {
        return jpaRepository.findByTenantIdAndId(tenantId.value(), id).map(mapper::toDomain);
    }

    @Override
    public List<ComplianceProfile> findByTenantAndType(TenantId tenantId, ComplianceProfileType type) {
        return jpaRepository.findByTenantIdAndType(tenantId.value(), type.name()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
