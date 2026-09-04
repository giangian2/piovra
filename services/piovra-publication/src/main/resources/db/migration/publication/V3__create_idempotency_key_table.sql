-- Backs @Idempotent on ProductChangedHandler.handle(): "receiving the same event twice" is Kafka's
-- normal mode of operation, not an edge case (docs/12-development-guidelines.md section 6).
CREATE TABLE publication.idempotency_key (
    key         TEXT PRIMARY KEY,
    claimed_at  TIMESTAMPTZ NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX ON publication.idempotency_key (expires_at);
