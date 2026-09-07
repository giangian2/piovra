-- Stock projection + append-only movement ledger (docs/02-services.md, "inventory-service").
-- available = max(0, on_hand - reserved - buffer), enforced by the domain, not the database.
CREATE TABLE inventory.stock_level (
    id          UUID PRIMARY KEY,
    tenant_id   TEXT NOT NULL,
    sku         TEXT NOT NULL,
    on_hand     INT NOT NULL DEFAULT 0,
    reserved    INT NOT NULL DEFAULT 0,
    buffer      INT NOT NULL DEFAULT 0,
    available   INT NOT NULL DEFAULT 0,
    version     BIGINT NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, sku)
);

-- idempotency_key is scoped per tenant, not globally unique: docs/02's schema sketch omits tenant_id
-- for brevity, but an order/line pair or a feed batch id is not guaranteed unique across tenants.
CREATE TABLE inventory.stock_movement (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       TEXT NOT NULL,
    sku             TEXT NOT NULL,
    delta           INT NOT NULL,
    reason          TEXT NOT NULL,
    source_channel  TEXT,
    idempotency_key TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, idempotency_key)
);
CREATE INDEX ON inventory.stock_movement (tenant_id, sku, created_at);
