package dev.piovra.events;

import java.time.Instant;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.TenantId;
import dev.piovra.model.order.CanonicalOrder;
import dev.piovra.model.order.OrderStatus;

/** An order transitioned to a different status (docs/02-services.md, "order-service"). */
public record OrderStatusChanged(
        String eventId,
        TenantId tenantId,
        String orderId,
        ChannelId channelId,
        String channelOrderId,
        OrderStatus previousStatus,
        OrderStatus newStatus,
        Instant occurredAt)
        implements DomainEvent {

    public static OrderStatusChanged of(CanonicalOrder before, OrderStatus newStatus) {
        return new OrderStatusChanged(
                Ids.newId(),
                before.tenantId(),
                before.orderId(),
                before.channelId(),
                before.channelOrderId(),
                before.status(),
                newStatus,
                Instant.now());
    }

    @Override
    public String partitionKey() {
        return tenantId + "|" + orderId;
    }

    @Override
    public String topic() {
        return Topics.ORDER_STATUS_CHANGED;
    }
}
