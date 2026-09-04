-- Transactional outbox (docs/12-development-guidelines.md section 5.4).
CREATE TABLE publication.outbox_event (
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
CREATE INDEX ON publication.outbox_event (status, created_at);
