package dev.piovra.outbox;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    /** Exhausted {@code maxAttempts}: no longer picked up by the relay. Inspect via SQL for now. */
    FAILED
}
