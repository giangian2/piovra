package dev.piovra.driver.spi;

import dev.piovra.common.ErrorClass;
import java.time.Duration;
import java.util.Optional;

/**
 * An error translated into the canonical taxonomy.
 *
 * @param code canonical code, e.g. EBAY_MARKETPLACE_REJECT_25002
 * @param nativeCode the marketplace's original code, kept for diagnosis
 * @param message a message an operator can understand, not a stack trace
 * @param suggestedAction what a human must do to fix it. If this is null on a permanent error,
 *     either it is our bug or a translation is missing: that is a code review criterion.
 * @param retryAfter set on RATE_LIMIT errors when the marketplace reports it
 */
public record DriverError(
        ErrorClass errorClass,
        String code,
        String nativeCode,
        String message,
        String suggestedAction,
        Integer httpStatus,
        Duration retryAfter) {

    public static DriverError of(ErrorClass errorClass, String code, String message) {
        return new DriverError(errorClass, code, null, message, null, null, null);
    }

    public static DriverError internal(String message, Exception cause) {
        return new DriverError(
                ErrorClass.INTERNAL,
                "INTERNAL",
                null,
                message + ": " + cause.getClass().getSimpleName() + " " + cause.getMessage(),
                null,
                null,
                null);
    }

    public boolean isRetryable() {
        return errorClass.isRetryable();
    }

    public Optional<Duration> retryAfterHint() {
        return Optional.ofNullable(retryAfter);
    }
}
