package dev.piovra.crosscutting.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a call towards a marketplace API.
 *
 * <p>One single place collects everything that has to happen around EVERY external call: duration
 * metric with outcome, structured log, MDC, a row in {@code sync_attempt}. Written by hand, this
 * block would be duplicated across a couple of dozen methods and would diverge within a month;
 * worse, adding a dimension to the metric would mean remembering all twenty of them.
 *
 * <p><b>Where it applies:</b> in the connector, on the method that invokes the driver. Never inside
 * the driver: the driver module does not know Spring, and {@code DriverIndependenceTest} enforces
 * that. The aspect sits at the boundary, not in the translator.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChannelCall {

    /** Logical operation, used as a metric tag: UPSERT, INVENTORY, PRICE, END, ORDER_FETCH. */
    String operation();

    /** When true the duration also feeds the percentile histogram (it costs memory: only where
     * needed). */
    boolean percentiles() default true;
}
