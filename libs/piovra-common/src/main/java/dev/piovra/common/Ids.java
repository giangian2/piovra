package dev.piovra.common;

import com.github.f4b6a3.ulid.UlidCreator;

/**
 * Technical identifiers. ULID rather than UUID v4: time-sortable, so B-tree indexes stay
 * unfragmented and logs read in chronological order.
 */
public final class Ids {

    private Ids() {}

    public static String newId() {
        return UlidCreator.getMonotonicUlid().toString();
    }

    /** Idempotency key of a stock movement: see docs/07-order-flow.md. */
    public static String stockMovementKey(ChannelId channel, String orderId, String lineId, String suffix) {
        return channel + ":" + orderId + ":" + lineId + (suffix == null ? "" : ":" + suffix);
    }

    /** Kafka key: guarantees that every event about a product lands on the same partition. */
    public static String partitionKey(TenantId tenant, Sku sku) {
        return tenant + "|" + sku;
    }
}
