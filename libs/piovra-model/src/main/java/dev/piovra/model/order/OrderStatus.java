package dev.piovra.model.order;

/**
 * Canonical statuses. Deliberately few: the marketplace's native status stays in
 * {@code channelStatus} and in the raw payload, while only what the system acts on lives here.
 */
public enum OrderStatus {
    NEW,
    PAID,
    SHIPPED,
    COMPLETED,
    /** Produces a positive stock movement. */
    CANCELLED,
    /** Produces a positive stock movement, possibly partial. */
    REFUNDED
}
