package dev.piovra.events;

import dev.piovra.common.TenantId;
import java.time.Instant;

/**
 * Base of every event. The partition key is part of the event's contract, not a producer's choice:
 * that is how every event about a product is guaranteed to land on the same partition
 * (docs/adr/0002-kafka-sku-key.md).
 */
public interface DomainEvent {

    String eventId();

    TenantId tenantId();

    Instant occurredAt();

    /** Kafka key of the message. */
    String partitionKey();

    /** Destination topic. */
    String topic();
}
