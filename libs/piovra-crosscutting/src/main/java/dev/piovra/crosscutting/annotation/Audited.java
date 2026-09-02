package dev.piovra.crosscutting.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The operation must be recorded in the append-only audit log: who, when, what, before and after.
 *
 * <p>It applies to actions a human performs or authorises — forced resync, manual stock adjustment,
 * DLQ replay, channel configuration change — not to automated traffic, which is already covered by
 * metrics and events.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /** Human-readable action: "product-resync", "stock-adjustment", "dlq-replay". */
    String action();

    /** SpEL identifying the affected object, e.g. "#sku.value()". */
    String target() default "";
}
