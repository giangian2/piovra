package dev.piovra.events;

import java.time.Instant;
import java.util.List;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.TenantId;
import dev.piovra.model.order.CanonicalOrder;
import dev.piovra.model.order.OrderLine;

/**
 * The order has been deduplicated, persisted and has its SKUs resolved. This is the signal that
 * authorises the inventory service to move stock.
 *
 * <p>Only resolved lines are here: UNMAPPED ones stay on the order but generate no movement until a
 * human creates the mapping.
 */
public record OrderAccepted(
        String eventId,
        TenantId tenantId,
        String orderId,
        ChannelId channelId,
        String channelOrderId,
        List<OrderLine> lines,
        /** True when the event comes from a cancellation or a return: the movements are positive. */
        boolean restoresStock,
        Instant occurredAt)
        implements DomainEvent {

    public OrderAccepted {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public static OrderAccepted from(CanonicalOrder order) {
        return new OrderAccepted(
                Ids.newId(),
                order.tenantId(),
                order.orderId(),
                order.channelId(),
                order.channelOrderId(),
                order.inventoryAffectingLines(),
                order.restoresStock(),
                Instant.now());
    }

    @Override
    public String partitionKey() {
        return tenantId + "|" + orderId;
    }

    @Override
    public String topic() {
        return Topics.ORDER_ACCEPTED;
    }
}
