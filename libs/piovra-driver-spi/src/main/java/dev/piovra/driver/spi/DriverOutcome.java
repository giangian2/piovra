package dev.piovra.driver.spi;

public enum DriverOutcome {
    SUCCESS,
    /** The driver verified there was nothing to do: no external call was made. */
    NOOP,
    /** Stale command: a higher revision has already been applied. Discarded without error. */
    STALE,
    RETRYABLE_ERROR,
    PERMANENT_ERROR
}
