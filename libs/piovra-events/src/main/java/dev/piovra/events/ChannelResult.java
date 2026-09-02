package dev.piovra.events;

import java.time.Instant;
import java.util.Map;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.driver.spi.DriverError;
import dev.piovra.driver.spi.DriverOutcome;

/**
 * Outcome of a command. It closes the loop back to the publication service, which updates the
 * listing state, the external id and the published snapshot.
 */
public record ChannelResult(
        String eventId,
        String commandId,
        TenantId tenantId,
        ChannelId channelId,
        Sku sku,
        DriverOutcome outcome,
        long revision,
        String externalId,
        Map<String, String> externalVariantIds,
        String publishedSnapshotHash,
        DriverError error,
        long latencyMs,
        int attempt,
        Instant occurredAt)
        implements DomainEvent {

    public ChannelResult {
        externalVariantIds = externalVariantIds == null ? Map.of() : Map.copyOf(externalVariantIds);
    }

    public static ChannelResult success(
            ChannelCommand command, String externalId, Map<String, String> variantIds, String snapshotHash, long ms) {
        return new ChannelResult(
                Ids.newId(),
                command.commandId(),
                command.tenantId(),
                command.channelId(),
                command.sku(),
                DriverOutcome.SUCCESS,
                command.revision(),
                externalId,
                variantIds,
                snapshotHash,
                null,
                ms,
                command.attempt(),
                Instant.now());
    }

    public static ChannelResult failure(ChannelCommand command, DriverError error, long ms) {
        DriverOutcome outcome = error.isRetryable() ? DriverOutcome.RETRYABLE_ERROR : DriverOutcome.PERMANENT_ERROR;
        return new ChannelResult(
                Ids.newId(),
                command.commandId(),
                command.tenantId(),
                command.channelId(),
                command.sku(),
                outcome,
                command.revision(),
                null,
                Map.of(),
                null,
                error,
                ms,
                command.attempt(),
                Instant.now());
    }

    public static ChannelResult stale(ChannelCommand command) {
        return new ChannelResult(
                Ids.newId(),
                command.commandId(),
                command.tenantId(),
                command.channelId(),
                command.sku(),
                DriverOutcome.STALE,
                command.revision(),
                null,
                Map.of(),
                null,
                null,
                0,
                command.attempt(),
                Instant.now());
    }

    @Override
    public String partitionKey() {
        return Ids.partitionKey(tenantId, sku);
    }

    @Override
    public String topic() {
        return Topics.CHANNEL_RESULT;
    }
}
