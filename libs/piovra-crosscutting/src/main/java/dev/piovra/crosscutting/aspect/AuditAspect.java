package dev.piovra.crosscutting.aspect;

import java.time.Instant;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;

import dev.piovra.crosscutting.MdcKeys;
import dev.piovra.crosscutting.annotation.Audited;
import dev.piovra.crosscutting.port.AuditSink;

/**
 * Writes the actions annotated with {@link Audited} into the audit log.
 *
 * <p>It records <b>failed</b> attempts too: "who tried to do what and did not manage" is often the
 * single most useful piece of information during an incident.
 */
@Aspect
public class AuditAspect {

    private final AuditSink sink;
    private final SpelKeyResolver keys = new SpelKeyResolver();

    public AuditAspect(AuditSink sink) {
        this.sink = sink;
    }

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        String target = keys.resolve(joinPoint, audited.target());
        String tenant = MDC.get(MdcKeys.TENANT);
        String actor = MDC.get("actor");
        try {
            Object result = joinPoint.proceed();
            sink.record(new AuditSink.AuditEntry(audited.action(), target, actor, tenant, true, null, Instant.now()));
            return result;
        } catch (Throwable failure) {
            sink.record(new AuditSink.AuditEntry(
                    audited.action(),
                    target,
                    actor,
                    tenant,
                    false,
                    failure.getClass().getSimpleName() + ": " + failure.getMessage(),
                    Instant.now()));
            throw failure;
        }
    }
}
