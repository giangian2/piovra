package dev.piovra.events;

import java.time.Instant;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;

/**
 * The available stock of a variant changed. This is the event that closes the anti-oversell loop:
 * the fan-out to every channel starts here (docs/07-order-flow.md).
 *
 * @param sourceChannelId the channel that originated the movement, null for feeds and manual
 *     adjustments. It avoids pointlessly republishing to the channel that just sold for us.
 */
public record InventoryChanged(
        String eventId,
        TenantId tenantId,
        Sku sku,
        int available,
        int previousAvailable,
        Reason reason,
        ChannelId sourceChannelId,
        long movementId,
        Instant occurredAt)
        implements DomainEvent {

    public enum Reason {
        FEED_SET,
        ORDER,
        CANCEL,
        RETURN,
        MANUAL,
        RECONCILE
    }

    public static InventoryChanged of(
            TenantId tenant, Sku sku, int available, int previous, Reason reason, ChannelId source, long movementId) {
        return new InventoryChanged(
                Ids.newId(), tenant, sku, available, previous, reason, source, movementId, Instant.now());
    }

    /** Dropping below the critical threshold justifies immediate propagation without batching. */
    public boolean isCritical(int threshold) {
        return available <= threshold && available < previousAvailable;
    }

    @Override
    public String partitionKey() {
        return Ids.partitionKey(tenantId, sku);
    }

    @Override
    public String topic() {
        return Topics.INVENTORY_CHANGED;
    }
}
