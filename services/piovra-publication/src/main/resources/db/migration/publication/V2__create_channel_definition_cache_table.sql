-- Publication's own local read-model of channel configuration (docs/02-services.md), fed by
-- consuming the compacted channel.config.v1 topic - it cannot call channel-config directly.
CREATE TABLE publication.channel_definition_cache (
    id          UUID PRIMARY KEY,
    tenant_id   TEXT NOT NULL,
    channel_id  TEXT NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT true,
    payload     JSONB NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, channel_id)
);
CREATE INDEX ON publication.channel_definition_cache (tenant_id, enabled);
