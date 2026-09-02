package dev.piovra.events;

import java.time.Instant;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.TenantId;
import dev.piovra.model.order.CanonicalOrder;

/**
 * A driver saw an order on the marketplace. SKUs are not resolved yet: resolution belongs to the
 * order service, which owns the mapping.
 *
 * <p>The partition key is the order, not the SKU: orders are independent of one another and need no
 * per-product ordering.
 */
public record OrderReceived(
        String eventId,
        TenantId tenantId,
        ChannelId channelId,
        String channelOrderId,
        CanonicalOrder order,
        /** Reference to the original payload in object storage. It contains personal data. */
        String rawPayloadUri,
        Instant occurredAt)
        implements DomainEvent {

    public static OrderReceived of(CanonicalOrder order, String rawPayloadUri) {
        return new OrderReceived(
                Ids.newId(),
                order.tenantId(),
                order.channelId(),
                order.channelOrderId(),
                order,
                rawPayloadUri,
                Instant.now());
    }

    @Override
    public String partitionKey() {
        return tenantId + "|" + channelId + "|" + channelOrderId;
    }

    @Override
    public String topic() {
        return Topics.CHANNEL_ORDER_RECEIVED;
    }
}
