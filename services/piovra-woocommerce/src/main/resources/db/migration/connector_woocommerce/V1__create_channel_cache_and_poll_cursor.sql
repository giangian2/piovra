-- Local read-model of channel configuration, fed by consuming the compacted channel.config.v1
-- topic: the connector cannot call channel-config (ArchitectureTest.services_do_not_call_each_other),
-- and per docs/02-services.md that is the intended integration. Same shape as
-- publication.channel_definition_cache.
CREATE TABLE connector_woocommerce.channel_definition_cache (
    id         UUID PRIMARY KEY,
    tenant_id  TEXT        NOT NULL,
    channel_id TEXT        NOT NULL,
    type       TEXT        NOT NULL,
    enabled    BOOLEAN     NOT NULL,
    payload    JSONB       NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, channel_id)
);
CREATE INDEX ON connector_woocommerce.channel_definition_cache (type, enabled);

-- Polling cursor AND distributed lease in the same row.
--
-- The row has to exist anyway to persist the cursor, so SELECT ... FOR UPDATE SKIP LOCKED gives
-- both the lease acquisition and the cursor read in one query - no ShedLock, no "poll requested"
-- topic (docs/07-order-flow.md section 2.1).
--
-- Two mechanisms on purpose: SKIP LOCKED settles the instantaneous race between replicas reaching
-- for the same channel, while leased_until is the logical lease that OUTLIVES the transaction and
-- covers the whole HTTP round trip - a transaction must never stay open across a marketplace call
-- (docs/12-development-guidelines.md section 5.4). If the process dies, the lease expires by itself.
CREATE TABLE connector_woocommerce.poll_cursor (
    tenant_id            TEXT        NOT NULL,
    channel_id           TEXT        NOT NULL,
    poll_kind            TEXT        NOT NULL,      -- ORDERS today; LISTINGS when reconciliation lands
    cursor_at            TIMESTAMPTZ NOT NULL,      -- high-water mark on date_modified_gmt
    page_cursor          TEXT,                      -- driver pagination token mid-scan
    leased_until         TIMESTAMPTZ,               -- NULL = free
    leased_by            TEXT,
    last_success_at      TIMESTAMPTZ,
    last_error           TEXT,
    consecutive_failures INT         NOT NULL DEFAULT 0,
    updated_at           TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (tenant_id, channel_id, poll_kind)
);
CREATE INDEX ON connector_woocommerce.poll_cursor (poll_kind, leased_until);
