-- Transactional outbox (docs/12-development-guidelines.md section 5.4): the row is written in the
-- same local transaction as the channel_definition write, then relayed to Kafka by OutboxRelay.
CREATE TABLE channel_config.outbox_event (
    id            TEXT PRIMARY KEY,
    partition_key TEXT NOT NULL,
    topic         TEXT NOT NULL,
    event_type    TEXT NOT NULL,
    payload       JSONB NOT NULL,
    headers       JSONB NOT NULL,
    status        TEXT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    published_at  TIMESTAMPTZ,
    attempts      INT NOT NULL DEFAULT 0,
    last_error    TEXT
);
CREATE INDEX ON channel_config.outbox_event (status, created_at);
