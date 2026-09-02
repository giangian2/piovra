/**
 * Contract of the marketplace drivers.
 *
 * <p>Architectural rule, verified by ArchUnit: this package must not depend on Spring, on Kafka or
 * on any service. It depends only on {@code piovra-model} and {@code piovra-common}. That is what
 * makes a driver usable from a test, a CLI or a batch job, and replaceable without touching the
 * core.
 */
package dev.piovra.driver.spi;
