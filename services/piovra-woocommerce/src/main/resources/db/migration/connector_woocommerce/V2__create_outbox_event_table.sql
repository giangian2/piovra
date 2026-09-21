-- Transactional outbox (docs/12-development-guidelines.md section 5.4): the orders a poll read and
-- the cursor advance commit together, so a crash re-reads a page instead of losing it.
CREATE TABLE connector_woocommerce.outbox_event (
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
CREATE INDEX ON connector_woocommerce.outbox_event (status, created_at);
CREATE INDEX ON connector_woocommerce.outbox_event (status, next_retry_at);
