package dev.piovra.model.channel;

import java.math.BigDecimal;

/**
 * Publishing policy of a channel. It travels on the compacted configuration topic, so it lives in
 * the shared model rather than inside channel-config: the publication service must be able to read
 * it without calling anyone.
 *
 * @param stockBuffer safety stock that is never published. This is the primary defence against
 *     overselling on fast channels: we publish available - buffer.
 * @param maxPublishableQty cap on the published quantity; many marketplaces dislike huge numbers,
 *     and advertising 9999 units helps nobody
 * @param priceAdjustmentPercent per-channel markup or discount, e.g. +8.0 to cover fees
 * @param endOnZero if true, zero quantity means delisting rather than a listing at zero
 * @param criticalStockThreshold below this level propagation is immediate, without batching
 */
public record ChannelPolicy(
        int stockBuffer,
        int maxPublishableQty,
        BigDecimal priceAdjustmentPercent,
        boolean endOnZero,
        int criticalStockThreshold,
        boolean requiresManualApproval) {

    public static final ChannelPolicy DEFAULT = new ChannelPolicy(0, 99, BigDecimal.ZERO, false, 3, false);

    /** Quantity actually publishable: never negative, never above the cap. */
    public int publishableQuantity(int available) {
        return Math.clamp((long) available - stockBuffer, 0, maxPublishableQty);
    }

    public boolean isCritical(int available) {
        return publishableQuantity(available) <= criticalStockThreshold;
    }
}
