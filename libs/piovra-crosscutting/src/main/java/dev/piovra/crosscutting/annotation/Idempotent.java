package dev.piovra.crosscutting.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The method must execute exactly once for a given key, even if the triggering event arrives twice.
 *
 * <p>Kafka delivers at-least-once: <b>every</b> consumer in the system has this problem. Solving it
 * once in an aspect, rather than with an {@code if (alreadySeen) return;} repeated everywhere, is
 * exactly the use case AOP exists for — the logic is identical everywhere and has nothing to do
 * with the domain of the method it protects.
 *
 * <p>The key is a SpEL expression over the method arguments:
 *
 * <pre>{@code
 * @Idempotent(key = "'cmd:' + #command.commandId()")
 * public void handle(ChannelCommand command) { ... }
 * }</pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** SpEL expression evaluated over the method arguments. It must produce a stable key. */
    String key();

    /** How long to remember the execution. Beyond this window a duplicate would run again. */
    String ttl() default "P7D";
}
