package dev.piovra.order.domain.service;

import java.util.Optional;

import dev.piovra.common.Sku;
import dev.piovra.model.order.LineResolution;
import dev.piovra.model.order.OrderLine;

/**
 * Turns a resolved (or not) SKU into the resulting {@link OrderLine} - pure, no I/O
 * (docs/12-development-guidelines.md section 4). The caller does the actual resolving (parsing
 * {@code channelSku} and checking {@code KnownSkuRepository}); this only decides the outcome.
 *
 * <p>Deliberately binary (MAPPED/UNMAPPED only): {@code AMBIGUOUS} would require resolving against
 * {@code external_variant_ids}, data no real driver produces yet (docs/07-order-flow.md section 3).
 */
public final class LineResolver {

    private LineResolver() {}

    public static OrderLine resolve(OrderLine raw, Optional<Sku> resolvedSku) {
        return resolvedSku
                .map(sku -> withResolution(raw, LineResolution.MAPPED, sku))
                .orElseGet(() -> withResolution(raw, LineResolution.UNMAPPED, null));
    }

    private static OrderLine withResolution(OrderLine raw, LineResolution resolution, Sku sku) {
        return new OrderLine(
                raw.lineId(), raw.channelLineId(), raw.channelSku(), sku, resolution, raw.quantity(), raw.unitPrice());
    }
}
