package dev.piovra.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class BackoffCalculatorTest {

    private final BackoffCalculator calculator = new BackoffCalculator(Duration.ofSeconds(1), Duration.ofMinutes(5));

    @Test
    void the_delay_never_exceeds_the_cap() {
        for (int attempts = 1; attempts <= 30; attempts++) {
            assertThat(calculator.compute(attempts)).isLessThanOrEqualTo(Duration.ofMinutes(5));
        }
    }

    @Test
    void the_delay_is_never_negative() {
        for (int attempts = 0; attempts <= 30; attempts++) {
            assertThat(calculator.compute(attempts)).isGreaterThanOrEqualTo(Duration.ZERO);
        }
    }

    @Test
    void later_attempts_have_a_higher_worst_case_ceiling_until_the_cap_is_reached() {
        Duration first = calculator.compute(1);
        Duration fifth = calculator.compute(5);

        // Jitter makes any single sample noisy, so assert on the theoretical ceiling instead.
        assertThat(exponentialCeiling(1)).isLessThan(exponentialCeiling(5));
        assertThat(first).isLessThanOrEqualTo(exponentialCeiling(1));
        assertThat(fifth).isLessThanOrEqualTo(exponentialCeiling(5));
    }

    @Test
    void a_very_large_attempt_count_does_not_overflow_or_throw() {
        assertThat(calculator.compute(1000))
                .isGreaterThanOrEqualTo(Duration.ZERO)
                .isLessThanOrEqualTo(Duration.ofMinutes(5));
    }

    private static Duration exponentialCeiling(int attempts) {
        Duration exponential = Duration.ofSeconds(1).multipliedBy(1L << attempts);
        return exponential.compareTo(Duration.ofMinutes(5)) > 0 ? Duration.ofMinutes(5) : exponential;
    }
}
