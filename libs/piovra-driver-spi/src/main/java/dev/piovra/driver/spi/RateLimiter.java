package dev.piovra.driver.spi;

import java.time.Duration;

/**
 * Limits the call rate towards a channel. Declared here as an interface so the driver can use it
 * without knowing about Redis: in tests it is a no-op, in production it is a distributed token
 * bucket shared by every replica of the connector.
 */
public interface RateLimiter {

    /**
     * Waits, if necessary, for permission to make {@code permits} calls.
     *
     * @return true if permission was granted within {@code timeout}
     */
    boolean acquire(int permits, Duration timeout) throws InterruptedException;

    default boolean acquire(Duration timeout) throws InterruptedException {
        return acquire(1, timeout);
    }

    /** The marketplace answered 429 with a Retry-After: the driver passes it on to the bucket. */
    void penalize(Duration retryAfter);

    /** Implementation for tests and local environments. */
    static RateLimiter unlimited() {
        return new RateLimiter() {
            @Override
            public boolean acquire(int permits, Duration timeout) {
                return true;
            }

            @Override
            public void penalize(Duration retryAfter) {
                // no-op
            }
        };
    }
}
