package dev.piovra.publication.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryCacheJpaRepository extends JpaRepository<InventoryCacheEntity, UUID> {

    Optional<InventoryCacheEntity> findByTenantIdAndSku(String tenantId, String sku);

    List<InventoryCacheEntity> findByTenantIdAndSkuIn(String tenantId, Collection<String> skus);
}
