package dev.piovra.publication.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.publication.application.port.out.InventoryCache;

@Repository
public class InventoryCacheAdapter implements InventoryCache {

    private final InventoryCacheJpaRepository jpaRepository;

    public InventoryCacheAdapter(InventoryCacheJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Map<Sku, Integer> availableFor(TenantId tenantId, List<Sku> skus) {
        List<String> values = skus.stream().map(Sku::value).toList();
        return jpaRepository.findByTenantIdAndSkuIn(tenantId.value(), values).stream()
                .collect(Collectors.toMap(entity -> Sku.of(entity.sku()), InventoryCacheEntity::available));
    }

    @Override
    public void upsert(TenantId tenantId, Sku sku, int available) {
        InventoryCacheEntity entity = jpaRepository
                .findByTenantIdAndSku(tenantId.value(), sku.value())
                .map(existing -> {
                    existing.update(available);
                    return existing;
                })
                .orElseGet(() -> new InventoryCacheEntity(UUID.randomUUID(), tenantId.value(), sku.value(), available));
        jpaRepository.save(entity);
    }
}
