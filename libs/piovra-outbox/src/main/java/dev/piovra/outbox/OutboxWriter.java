package dev.piovra.outbox;

import dev.piovra.events.DomainEvent;

/**
 * The port a service uses to publish an event reliably: appending to the outbox inside the caller's
 * own transaction, never calling Kafka directly from application code (docs/12-development-guidelines.md
 * section 5.4).
 */
public interface OutboxWriter {

    void append(DomainEvent event);
}
