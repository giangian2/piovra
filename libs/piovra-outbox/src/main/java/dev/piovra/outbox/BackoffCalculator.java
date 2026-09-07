package dev.piovra.outbox;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Exponential backoff with full jitter, scaled for the relay's poll interval (milliseconds), not the
 * minutes-scale consumer-retry schedule of docs/09-errors-observability.md section 2 - that is a
 * different failure domain (a consumer that already received a message vs. a producer that could not
 * hand one to the broker).
 */
final class BackoffCalculator {

    private final Duration base;
    private final Duration cap;

    BackoffCalculator() {
        this(Duration.ofSeconds(1), Duration.ofMinutes(5));
    }

    BackoffCalculator(Duration base, Duration cap) {
        this.base = base;
        this.cap = cap;
    }

    /** {@code attempts} is the failure count so far (1 after the first failure). */
    Duration compute(int attempts) {
        long exponentialMillis = safeShiftLeft(base.toMillis(), attempts);
        long capped = Math.min(cap.toMillis(), exponentialMillis);
        long jittered = capped <= 0 ? 0 : ThreadLocalRandom.current().nextLong(capped);
        return Duration.ofMillis(jittered);
    }

    private static long safeShiftLeft(long base, int attempts) {
        if (attempts >= 62) {
            return Long.MAX_VALUE;
        }
        long shifted = base << attempts;
        return shifted < 0 ? Long.MAX_VALUE : shifted;
    }
}
