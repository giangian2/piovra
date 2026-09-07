package dev.piovra.inventory.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface StockLevelJpaRepository extends JpaRepository<StockLevelEntity, UUID> {

    Optional<StockLevelEntity> findByTenantIdAndSku(String tenantId, String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from StockLevelEntity e where e.tenantId = :tenantId and e.sku = :sku")
    Optional<StockLevelEntity> findForUpdate(String tenantId, String sku);
}
