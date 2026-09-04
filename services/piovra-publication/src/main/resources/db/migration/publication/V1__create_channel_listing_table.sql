-- The heart of the upsert: docs/03-data-model.md section 3, replicated almost verbatim.
-- last_command_id is TEXT (a ULID string, dev.piovra.common.Ids), not UUID as the doc's example
-- shows, since Ids.newId() produces ULIDs rather than RFC-4122 UUIDs.
CREATE TABLE publication.channel_listing (
    id                   UUID PRIMARY KEY,
    tenant_id            TEXT NOT NULL,
    sku                  TEXT NOT NULL,
    channel_id           TEXT NOT NULL,
    external_id          TEXT,
    external_variant_ids JSONB,
    state                TEXT NOT NULL,
    published_revision   BIGINT NOT NULL DEFAULT 0,
    published_snapshot   JSONB,
    snapshot_hash        TEXT,
    field_hashes         JSONB,
    last_command_id      TEXT,
    last_error_code      TEXT,
    last_error_message   TEXT,
    last_attempt_at      TIMESTAMPTZ,
    last_success_at      TIMESTAMPTZ,
    retry_count          INT NOT NULL DEFAULT 0,
    next_retry_at        TIMESTAMPTZ,
    UNIQUE (tenant_id, sku, channel_id)
);
CREATE INDEX ON publication.channel_listing (state, next_retry_at);
CREATE INDEX ON publication.channel_listing (channel_id, state);
