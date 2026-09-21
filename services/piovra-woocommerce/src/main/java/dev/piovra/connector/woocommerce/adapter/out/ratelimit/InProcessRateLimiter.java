package dev.piovra.connector.woocommerce.adapter.out.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

import dev.piovra.driver.spi.RateLimiter;

/**
 * A token bucket held in this process.
 *
 * <p>Deliberately not the Redis bucket the configuration comment promises: polling is one request
 * per channel every couple of minutes, so a cluster-wide budget buys nothing until the outbound
 * write path starts competing for the same quota. The SPI interface is what makes that a later
 * swap rather than a rewrite.
 *
 * <p>{@link ReentrantLock} rather than {@code synchronized}: the project's concurrency rule
 * (docs/12-development-guidelines.md section 5.3), and it gives {@code tryLock} with a timeout.
 */
public class InProcessRateLimiter implements RateLimiter {

    private final ReentrantLock lock = new ReentrantLock();
    private final double refillPerSecond;
    private final int burst;

    private double tokens;
    private Instant lastRefill = Instant.now();
    private Instant penalizedUntil = Instant.EPOCH;

    public InProcessRateLimiter(double refillPerSecond, int burst) {
        this.refillPerSecond = refillPerSecond;
        this.burst = burst;
        this.tokens = burst;
    }

    @Override
    public boolean acquire(int permits, Duration timeout) throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (true) {
            Duration wait = tryTake(permits);
            if (wait == null) {
                return true;
            }
            Instant wakeAt = Instant.now().plus(wait);
            if (wakeAt.isAfter(deadline)) {
                return false;
            }
            Thread.sleep(wait.toMillis() + 1);
        }
    }

    /** @return null when the permits were taken, otherwise how long to wait before retrying */
    private Duration tryTake(int permits) {
        lock.lock();
        try {
            Instant now = Instant.now();
            if (now.isBefore(penalizedUntil)) {
                return Duration.between(now, penalizedUntil);
            }
            refill(now);
            if (tokens >= permits) {
                tokens -= permits;
                return null;
            }
            double missing = permits - tokens;
            return Duration.ofMillis((long) Math.ceil(missing / refillPerSecond * 1000));
        } finally {
            lock.unlock();
        }
    }

    private void refill(Instant now) {
        double elapsedSeconds = Duration.between(lastRefill, now).toNanos() / 1_000_000_000d;
        tokens = Math.min(burst, tokens + elapsedSeconds * refillPerSecond);
        lastRefill = now;
    }

    /** The marketplace said 429 with a Retry-After: believe it rather than our own arithmetic. */
    @Override
    public void penalize(Duration retryAfter) {
        lock.lock();
        try {
            penalizedUntil = Instant.now().plus(retryAfter);
            tokens = 0;
        } finally {
            lock.unlock();
        }
    }
}
