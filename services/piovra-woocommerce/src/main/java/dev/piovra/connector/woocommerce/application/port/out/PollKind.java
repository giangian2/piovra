package dev.piovra.connector.woocommerce.application.port.out;

/** What a cursor is tracking. Only orders today; listing reconciliation reuses the same table. */
public enum PollKind {
    ORDERS,
    LISTINGS
}
