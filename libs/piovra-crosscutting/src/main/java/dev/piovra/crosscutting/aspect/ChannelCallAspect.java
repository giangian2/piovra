package dev.piovra.crosscutting.aspect;

import dev.piovra.crosscutting.MdcKeys;
import dev.piovra.crosscutting.annotation.ChannelCall;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Instruments calls to marketplace APIs.
 *
 * <p>It produces {@code piovra_marketplace_call_duration_seconds{channel,operation,outcome}}, the
 * metric that reveals saturation, latency and error rate per channel
 * (docs/09-errors-observability.md). Without an aspect this instrumentation would be repeated in
 * every connector method and would diverge: it is the textbook cross-cutting concern, identical
 * everywhere and orthogonal to the domain.
 *
 * <p>The channel is read from the MDC, already populated by the Kafka consumer: the aspect does not
 * need to inspect arguments and stays independent of the signatures it intercepts.
 */
@Aspect
public class ChannelCallAspect {

    private static final Logger log = LoggerFactory.getLogger(ChannelCallAspect.class);
    private static final String METRIC = "piovra.marketplace.call";

    private final MeterRegistry registry;

    public ChannelCallAspect(MeterRegistry registry) {
        this.registry = registry;
    }

    @Around("@annotation(channelCall)")
    public Object around(ProceedingJoinPoint joinPoint, ChannelCall channelCall) throws Throwable {
        String channel = MDC.get(MdcKeys.CHANNEL);
        long startNanos = System.nanoTime();
        String outcome = "success";
        try {
            return joinPoint.proceed();
        } catch (Throwable failure) {
            outcome = failure.getClass().getSimpleName();
            throw failure;
        } finally {
            Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
            Timer.Builder timer = Timer.builder(METRIC)
                    .tag("channel", channel == null ? "unknown" : channel)
                    .tag("operation", channelCall.operation())
                    .tag("outcome", outcome);
            if (channelCall.percentiles()) {
                timer.publishPercentileHistogram();
            }
            timer.register(registry).record(elapsed);

            if (log.isDebugEnabled()) {
                log.debug(
                        "call {} on {} finished in {} ms with outcome {}",
                        channelCall.operation(),
                        channel,
                        elapsed.toMillis(),
                        outcome);
            }
        }
    }
}
