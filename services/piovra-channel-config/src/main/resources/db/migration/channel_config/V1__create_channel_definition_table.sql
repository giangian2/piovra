-- Single point-lookup table: every query is by (tenant_id, channel_id). The full
-- ChannelDefinition (policy, category mapping, settings) is kept as one JSONB payload rather than
-- a normalized schema (docs/12-development-guidelines.md, "keep it simple" - no join ever needed here).
CREATE TABLE channel_config.channel_definition (
    id          UUID PRIMARY KEY,
    tenant_id   TEXT NOT NULL,
    channel_id  TEXT NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT true,
    payload     JSONB NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, channel_id)
);
