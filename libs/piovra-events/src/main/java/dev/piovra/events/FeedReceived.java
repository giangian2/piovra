package dev.piovra.events;

import java.time.Instant;

import dev.piovra.common.TenantId;

/**
 * A feed has been ingested and archived. The payload does NOT travel on Kafka: the object storage
 * reference does, because a 2 GB CSV is not a message.
 *
 * @param sha256 fingerprint of the file: identifies identical re-submissions
 * @param mode DELTA or FULL, see docs/05-feed-flow.md
 */
public record FeedReceived(
        String eventId,
        TenantId tenantId,
        String feedId,
        String sourceId,
        String storageUri,
        String format,
        String mode,
        String sha256,
        long sizeBytes,
        Instant occurredAt)
        implements DomainEvent {

    @Override
    public String partitionKey() {
        return tenantId + "|" + feedId;
    }

    @Override
    public String topic() {
        return Topics.FEED_RECEIVED;
    }
}
