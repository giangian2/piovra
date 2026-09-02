package dev.piovra.model.order;

import dev.piovra.common.ChannelId;
import dev.piovra.common.TenantId;
import java.time.Instant;
import java.util.List;

/**
 * A normalized order. The pair (channelId, channelOrderId) is UNIQUE in the database: it is the
 * only defence needed against at-least-once delivery and against the polling window overlap
 * (docs/07-order-flow.md).
 */
public record CanonicalOrder(
        String orderId,
        TenantId tenantId,
        ChannelId channelId,
        String channelOrderId,
        OrderStatus status,
        /** Native marketplace status, kept for diagnosis. */
        String channelStatus,
        Instant placedAt,
        Instant lastModifiedAt,
        Buyer buyer,
        Address shippingAddress,
        OrderTotals totals,
        List<OrderLine> lines,
        /** True once the stock movements for this order have been applied. */
        boolean stockApplied) {

    public CanonicalOrder {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public boolean hasUnmappedLines() {
        return lines.stream().anyMatch(l -> l.resolution() == LineResolution.UNMAPPED);
    }

    public List<OrderLine> inventoryAffectingLines() {
        return lines.stream().filter(OrderLine::affectsInventory).toList();
    }

    /** The statuses that return goods to stock. */
    public boolean restoresStock() {
        return status == OrderStatus.CANCELLED || status == OrderStatus.REFUNDED;
    }
}
