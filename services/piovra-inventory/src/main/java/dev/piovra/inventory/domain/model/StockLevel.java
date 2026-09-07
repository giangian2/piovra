package dev.piovra.inventory.domain.model;

import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;

/**
 * The stock projection for one SKU (docs/02-services.md, "inventory-service"):
 * {@code available = max(0, onHand - reserved - buffer)}. Recomputed in the compact constructor so
 * no caller can ever construct an inconsistent instance (docs/12-development-guidelines.md section
 * 2.2 - invariants belong in the constructor, not in the callers).
 *
 * <p>{@code reserved}/{@code buffer} stay at zero in this iteration (no reservation or anti-oversell
 * buffer machinery yet) but are real columns, matching the schema docs/02 already specifies.
 */
public record StockLevel(
        TenantId tenantId, Sku sku, int onHand, int reserved, int buffer, int available, long version) {

    /** {@code available} is always derived - see the compact constructor - so callers never supply it. */
    public StockLevel(TenantId tenantId, Sku sku, int onHand, int reserved, int buffer, long version) {
        this(tenantId, sku, onHand, reserved, buffer, 0, version);
    }

    public StockLevel {
        available = Math.max(0, onHand - reserved - buffer);
    }

    public static StockLevel initial(TenantId tenantId, Sku sku) {
        return new StockLevel(tenantId, sku, 0, 0, 0, 0);
    }
}
