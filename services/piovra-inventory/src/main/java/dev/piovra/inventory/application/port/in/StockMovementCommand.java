package dev.piovra.inventory.application.port.in;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Sku;
import dev.piovra.events.InventoryChanged;

/**
 * One movement to apply. {@code quantity} is absolute for {@link StockMovementMode#SET}, signed for
 * {@link StockMovementMode#DELTA}. {@code sourceChannelId} is null for feeds and manual adjustments,
 * matching {@link InventoryChanged}'s own field of the same name.
 */
public record StockMovementCommand(
        Sku sku,
        StockMovementMode mode,
        int quantity,
        InventoryChanged.Reason reason,
        ChannelId sourceChannelId,
        String idempotencyKey) {}
