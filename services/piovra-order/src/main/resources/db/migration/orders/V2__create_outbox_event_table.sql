-- Transactional outbox (docs/12-development-guidelines.md section 5.4). Written after the outbox
-- DLQ/backoff work, so next_retry_at is here from the start - no separate retrofit migration needed
-- for this module, same as inventory's V2.
CREATE TABLE orders.outbox_event (
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
    last_error    TEXT,
    next_retry_at TIMESTAMPTZ
);
CREATE INDEX ON orders.outbox_event (status, created_at);
CREATE INDEX ON orders.outbox_event (status, next_retry_at);
