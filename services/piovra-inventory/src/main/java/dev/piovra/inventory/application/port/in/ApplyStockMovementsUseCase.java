package dev.piovra.inventory.application.port.in;

import java.util.List;

import dev.piovra.common.TenantId;
import dev.piovra.events.InventoryChanged;

public interface ApplyStockMovementsUseCase {

    /** Only commands whose idempotency key is new and whose application changed {@code available}
     * produce an entry in the returned list - a duplicate or a no-op movement produces nothing. */
    List<InventoryChanged> apply(TenantId tenantId, List<StockMovementCommand> commands);
}
