package dev.piovra.inventory.domain.service;

import dev.piovra.inventory.domain.model.StockLevel;

/**
 * Pure arithmetic over {@link StockLevel} - no I/O, testable in milliseconds
 * (docs/12-development-guidelines.md section 4).
 */
public final class StockLedger {

    private StockLedger() {}

    /** {@code onHand} can go negative (an order oversold before a feed reconciles it) - the record's
     * compact constructor already clamps {@code available} at zero, that is the only guard needed. */
    public static StockLevel applyDelta(StockLevel current, int delta) {
        return new StockLevel(
                current.tenantId(),
                current.sku(),
                current.onHand() + delta,
                current.reserved(),
                current.buffer(),
                current.version() + 1);
    }

    public static StockLevel applySet(StockLevel current, int newOnHand) {
        return new StockLevel(
                current.tenantId(),
                current.sku(),
                newOnHand,
                current.reserved(),
                current.buffer(),
                current.version() + 1);
    }

    public static boolean availableChanged(StockLevel before, StockLevel after) {
        return before.available() != after.available();
    }
}
