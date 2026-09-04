-- Single point-lookup table: every query is by (tenant_id, sku), never a join. The full
-- CanonicalProduct is kept as one JSONB payload rather than a normalized product/variant schema.
CREATE TABLE catalog.products (
    id          UUID PRIMARY KEY,
    tenant_id   TEXT NOT NULL,
    sku         TEXT NOT NULL,
    revision    BIGINT NOT NULL,
    status      TEXT NOT NULL,
    payload     JSONB NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, sku)
);
