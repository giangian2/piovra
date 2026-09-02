package dev.piovra.driver.spi;

import java.util.Map;

/**
 * @param externalId id assigned or confirmed by the marketplace. It must be stored BEFORE the
 *     command is considered successful: losing it means creating a duplicate on the next retry.
 */
public record UpsertResult(
        DriverOutcome outcome,
        String externalId,
        Map<String, String> externalVariantIds,
        DriverError error,
        long latencyMs) {

    public UpsertResult {
        externalVariantIds = externalVariantIds == null ? Map.of() : Map.copyOf(externalVariantIds);
    }

    public static UpsertResult success(String externalId, Map<String, String> variantIds, long latencyMs) {
        return new UpsertResult(DriverOutcome.SUCCESS, externalId, variantIds, null, latencyMs);
    }

    public static UpsertResult failure(DriverError error, long latencyMs) {
        DriverOutcome outcome = error.isRetryable() ? DriverOutcome.RETRYABLE_ERROR : DriverOutcome.PERMANENT_ERROR;
        return new UpsertResult(outcome, null, Map.of(), error, latencyMs);
    }

    public boolean isSuccess() {
        return outcome == DriverOutcome.SUCCESS;
    }
}
