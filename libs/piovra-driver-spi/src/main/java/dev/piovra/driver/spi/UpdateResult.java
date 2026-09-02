package dev.piovra.driver.spi;

import java.util.List;
import java.util.Map;

/**
 * Outcome of a potentially bulk operation. Failures are <b>per item</b>: in a batch of 25 eBay
 * offers, 3 may fail while the other 22 succeed. Treating a non-atomic batch as atomic is a classic
 * source of inconsistent data.
 */
public record UpdateResult(
        DriverOutcome outcome,
        int succeeded,
        /** SKU -> error, for the failed items only. */
        Map<String, DriverError> failures,
        long latencyMs) {

    public UpdateResult {
        failures = failures == null ? Map.of() : Map.copyOf(failures);
    }

    public static UpdateResult allSucceeded(int count, long latencyMs) {
        return new UpdateResult(DriverOutcome.SUCCESS, count, Map.of(), latencyMs);
    }

    public static UpdateResult partial(int succeeded, Map<String, DriverError> failures, long latencyMs) {
        boolean anyRetryable = failures.values().stream().anyMatch(DriverError::isRetryable);
        DriverOutcome outcome = failures.isEmpty()
                ? DriverOutcome.SUCCESS
                : anyRetryable ? DriverOutcome.RETRYABLE_ERROR : DriverOutcome.PERMANENT_ERROR;
        return new UpdateResult(outcome, succeeded, failures, latencyMs);
    }

    public static UpdateResult noop() {
        return new UpdateResult(DriverOutcome.NOOP, 0, Map.of(), 0);
    }

    public List<String> failedSkus() {
        return List.copyOf(failures.keySet());
    }
}
