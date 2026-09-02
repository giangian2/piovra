package dev.piovra.crosscutting.aspect;

import java.time.Duration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import dev.piovra.crosscutting.annotation.Idempotent;
import dev.piovra.crosscutting.port.IdempotencyStore;

/**
 * Makes a method annotated with {@link Idempotent} idempotent.
 *
 * <p>High order (runs first): there is no point measuring, tracing or retrying an execution that
 * has to be skipped.
 *
 * <p>The chosen semantics are worth spelling out, because this is where such mechanisms usually get
 * it wrong: the key is <b>released when the method fails</b>. A failed attempt must not prevent the
 * retry — otherwise a network timeout would turn into a permanently lost update.
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class IdempotencyAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyAspect.class);

    private final IdempotencyStore store;
    private final SpelKeyResolver keys = new SpelKeyResolver();

    public IdempotencyAspect(IdempotencyStore store) {
        this.store = store;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = keys.resolve(joinPoint, idempotent.key());
        Duration ttl = Duration.parse(idempotent.ttl());

        if (!store.claim(key, ttl)) {
            log.debug("execution skipped, key already seen: {}", key);
            return null;
        }
        try {
            return joinPoint.proceed();
        } catch (Throwable failure) {
            store.release(key);
            throw failure;
        }
    }
}
