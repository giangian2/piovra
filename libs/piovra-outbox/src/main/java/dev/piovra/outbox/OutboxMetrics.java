package dev.piovra.outbox;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Minimal instrumentation for the relay's dead-letter path, following {@code ChannelCallAspect}'s
 * constructor-injection style. {@code registry} is nullable: a module booted without {@code
 * spring-boot-starter-actuator} (e.g. a service's own standalone test context) simply gets no metric,
 * the same graceful degradation {@code CrossCuttingAutoConfiguration}'s
 * {@code @ConditionalOnBean(MeterRegistry.class)} applies elsewhere.
 */
class OutboxMetrics {

    private final Counter permanentFailures;

    OutboxMetrics(MeterRegistry registry, String moduleName) {
        this.permanentFailures = registry == null
                ? null
                : Counter.builder("piovra.outbox.failed.total")
                        .description("Outbox rows that exhausted their retry budget and were dead-lettered")
                        .tag("module", moduleName)
                        .register(registry);
    }

    void permanentFailure() {
        if (permanentFailures != null) {
            permanentFailures.increment();
        }
    }
}
