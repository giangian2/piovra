package dev.piovra.catalog.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceProfileJpaRepository extends JpaRepository<ComplianceProfileEntity, String> {

    Optional<ComplianceProfileEntity> findByTenantIdAndId(String tenantId, String id);

    List<ComplianceProfileEntity> findByTenantIdAndType(String tenantId, String type);
}
