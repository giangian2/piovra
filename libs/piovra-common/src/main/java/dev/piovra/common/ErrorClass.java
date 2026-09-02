package dev.piovra.common;

/**
 * Canonical error taxonomy (docs/09-errors-observability.md). Every driver translates the
 * marketplace's native errors into one of these classes: it is the class, not the native code, that
 * decides whether we retry.
 */
public enum ErrorClass {
    /** Invalid input data. Retrying will not help. */
    VALIDATION(false),
    /** Missing configuration: unmapped category, required aspect, unknown SKU. */
    MAPPING(false),
    /** Credentials expired or revoked: a human has to step in. */
    AUTH(false),
    /** Quota or rate limit exhausted. Retried, with a long wait. */
    RATE_LIMIT(true),
    /** Timeout, 5xx, connection reset. Retried with exponential backoff. */
    TRANSIENT(true),
    /** The marketplace refused on policy or duplication. Retrying yields the same refusal. */
    MARKETPLACE_REJECT(false),
    /** Resource modified elsewhere: re-read and retry once. */
    CONFLICT(true),
    /** Our own bug. Goes to the DLQ, with an alert. */
    INTERNAL(false);

    private final boolean retryable;

    ErrorClass(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
