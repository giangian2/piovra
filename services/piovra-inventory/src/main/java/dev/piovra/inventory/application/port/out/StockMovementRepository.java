package dev.piovra.inventory.application.port.out;

import java.util.Optional;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.events.InventoryChanged;

public interface StockMovementRepository {

    /** Appends a movement to the ledger. {@link Optional#empty()} means {@code idempotencyKey} was
     * already claimed by an earlier call (at-least-once redelivery, or a resubmitted feed batch) -
     * the caller must treat that as a no-op, never re-apply the delta. */
    Optional<Long> append(
            TenantId tenantId,
            Sku sku,
            int delta,
            InventoryChanged.Reason reason,
            ChannelId sourceChannelId,
            String idempotencyKey);
}
