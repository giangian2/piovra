package dev.piovra.crosscutting.port;

import java.time.Duration;

/**
 * Memory of executions that already happened, used by {@code @Idempotent}.
 *
 * <p>It is a port rather than a concrete class because the right implementation depends on the
 * context: a Postgres table in the connectors (it has to survive restarts), Redis elsewhere, an
 * in-memory map in tests. The aspect must not know which.
 */
public interface IdempotencyStore {

    /**
     * Registers the key if it was not there.
     *
     * @return true if the registration happened now, so the method must run; false if the key
     *     already existed, so the execution must be skipped
     */
    boolean claim(String key, Duration ttl);

    /**
     * Releases the key. Called when the execution fails, because a failed attempt must not prevent
     * the retry.
     */
    void release(String key);
}
