package dev.piovra.crosscutting.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.piovra.crosscutting.annotation.Idempotent;
import dev.piovra.crosscutting.port.IdempotencyStore;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

/**
 * The aspect is tested with a hand-built AspectJ proxy: no Spring context, tests in milliseconds.
 * If testing an aspect ever required booting a context, that would be the signal that the aspect is
 * doing too much.
 */
class IdempotencyAspectTest {

    /** In-memory stand-in for the port: a table in the connectors, a Set here. */
    static class InMemoryStore implements IdempotencyStore {
        final Set<String> claimed = new HashSet<>();

        @Override
        public boolean claim(String key, Duration ttl) {
            return claimed.add(key);
        }

        @Override
        public void release(String key) {
            claimed.remove(key);
        }
    }

    static class Handler {
        final AtomicInteger executions = new AtomicInteger();

        @Idempotent(key = "'cmd:' + #commandId")
        public void handle(String commandId) {
            executions.incrementAndGet();
        }

        @Idempotent(key = "'cmd:' + #commandId")
        public void handleFailing(String commandId) {
            executions.incrementAndGet();
            throw new IllegalStateException("the marketplace answered 503");
        }
    }

    private InMemoryStore store;
    private Handler handler;
    private Handler proxy;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        handler = new Handler();
        AspectJProxyFactory factory = new AspectJProxyFactory(handler);
        factory.addAspect(new IdempotencyAspect(store));
        proxy = factory.getProxy();
    }

    @Test
    void the_same_command_delivered_twice_runs_only_once() {
        proxy.handle("CMD-1");
        proxy.handle("CMD-1");

        assertThat(handler.executions.get()).isEqualTo(1);
    }

    @Test
    void different_commands_both_run() {
        proxy.handle("CMD-1");
        proxy.handle("CMD-2");

        assertThat(handler.executions.get()).isEqualTo(2);
    }

    @Test
    void a_failure_releases_the_key_so_the_retry_can_happen() {
        assertThatThrownBy(() -> proxy.handleFailing("CMD-3")).isInstanceOf(IllegalStateException.class);

        assertThat(store.claimed)
                .as("a failed attempt must not block the retry: that would be a lost update")
                .isEmpty();

        assertThatThrownBy(() -> proxy.handleFailing("CMD-3")).isInstanceOf(IllegalStateException.class);
        assertThat(handler.executions.get()).isEqualTo(2);
    }
}
