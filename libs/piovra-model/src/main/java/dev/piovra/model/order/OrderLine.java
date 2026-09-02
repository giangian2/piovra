package dev.piovra.model.order;

import dev.piovra.common.Money;
import dev.piovra.common.Sku;
import java.util.Optional;

public record OrderLine(
        String lineId,
        String channelLineId,
        /** SKU exactly as the marketplace reports it. */
        String channelSku,
        /** Resolved canonical SKU, null when {@link LineResolution#UNMAPPED}. */
        Sku sku,
        LineResolution resolution,
        int quantity,
        Money unitPrice) {

    public OrderLine {
        if (quantity <= 0) {
            throw new IllegalArgumentException("non-positive quantity on line " + lineId);
        }
    }

    /** Only resolved lines move stock. */
    public boolean affectsInventory() {
        return resolution == LineResolution.MAPPED && sku != null;
    }

    public Optional<Sku> canonicalSku() {
        return Optional.ofNullable(sku);
    }
}
