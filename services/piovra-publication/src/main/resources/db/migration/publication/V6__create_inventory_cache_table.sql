-- Publication's local read-model of available stock, fed by consuming inventory.changed.v1 - see
-- InventoryCache. Same rationale as V2 (channel_definition_cache): a service cannot call another
-- service directly, so it keeps its own cache.
CREATE TABLE publication.inventory_cache (
    id          UUID PRIMARY KEY,
    tenant_id   TEXT NOT NULL,
    sku         TEXT NOT NULL,
    available   INT NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, sku)
);
