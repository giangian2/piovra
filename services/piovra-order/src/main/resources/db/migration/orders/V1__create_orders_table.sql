-- Canonical orders, deduplicated on (tenant_id, channel_id, channel_order_id) - the only defence
-- needed against at-least-once delivery and polling-window overlap (docs/07-order-flow.md).
-- id is the order's own ULID (CanonicalOrder.orderId), not a separate surrogate UUID. Point lookup
-- by natural key, full CanonicalOrder kept as one JSONB payload, same reasoning as catalog.products.
CREATE TABLE orders.orders (
    id               TEXT PRIMARY KEY,
    tenant_id        TEXT NOT NULL,
    channel_id       TEXT NOT NULL,
    channel_order_id TEXT NOT NULL,
    status           TEXT NOT NULL,
    payload          JSONB NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, channel_id, channel_order_id)
);

-- Order-service's own local read-model of "SKUs the catalog knows about" (fed by consuming
-- catalog.product.changed) - existence-only, no payload: order-service cannot query catalog-service
-- directly.
CREATE TABLE orders.known_sku (
    tenant_id TEXT NOT NULL,
    sku       TEXT NOT NULL,
    UNIQUE (tenant_id, sku)
);
