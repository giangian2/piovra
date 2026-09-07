-- Backoff scheduling for the outbox relay: a failed row is no longer retried on every tick, it
-- waits until next_retry_at. NULL means "ready now" (never failed, or freshly inserted).
ALTER TABLE catalog.outbox_event ADD COLUMN next_retry_at TIMESTAMPTZ NULL;

CREATE INDEX ON catalog.outbox_event (status, next_retry_at);
