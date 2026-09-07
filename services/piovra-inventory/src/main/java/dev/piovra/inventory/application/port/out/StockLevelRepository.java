package dev.piovra.inventory.application.port.out;

import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.inventory.domain.model.StockLevel;

public interface StockLevelRepository {

    /** Locks the row for the caller's transaction, inserting a zeroed row first if it does not exist
     * yet - guarantees a caller always gets a lockable row, never an empty result. */
    StockLevel lockOrCreate(TenantId tenantId, Sku sku);

    void save(StockLevel level);

    /** Insert-if-absent, no lock held afterward - for the {@code ProductChanged} listener, which only
     * needs a row to exist, never mutates it. */
    void ensureExists(TenantId tenantId, Sku sku);
}
