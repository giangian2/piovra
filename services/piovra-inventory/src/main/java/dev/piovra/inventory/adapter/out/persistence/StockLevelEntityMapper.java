package dev.piovra.inventory.adapter.out.persistence;

import java.util.UUID;

import org.springframework.stereotype.Component;

import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.inventory.domain.model.StockLevel;

@Component
public class StockLevelEntityMapper {

    public StockLevelEntity toNewEntity(StockLevel level) {
        return new StockLevelEntity(
                UUID.randomUUID(),
                level.tenantId().value(),
                level.sku().value(),
                level.onHand(),
                level.reserved(),
                level.buffer(),
                level.available(),
                level.version());
    }

    public void applyTo(StockLevelEntity entity, StockLevel level) {
        entity.update(level.onHand(), level.reserved(), level.buffer(), level.available(), level.version());
    }

    public StockLevel toDomain(StockLevelEntity entity) {
        return new StockLevel(
                TenantId.of(entity.tenantId()),
                Sku.of(entity.sku()),
                entity.onHand(),
                entity.reserved(),
                entity.buffer(),
                entity.available(),
                entity.version());
    }
}
