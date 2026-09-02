package dev.piovra.events;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.model.channel.ChannelType;
import dev.piovra.model.channel.FieldGroup;

/**
 * A command addressed to a driver. Unlike an event it is not a past fact: it is an intent with a
 * specific recipient.
 *
 * @param commandId idempotency key. A driver that already executed it re-emits the previous outcome
 *     without calling the marketplace again.
 * @param revision canonical revision of the product. The driver discards as STALE any command whose
 *     revision is below the one already applied: that is what makes retries and residual reordering
 *     harmless.
 * @param changedGroups modified field groups, used to pick the lightest API call
 * @param payload canonical projection already computed for this channel (overrides and policy
 *     applied)
 */
public record ChannelCommand(
        String commandId,
        TenantId tenantId,
        ChannelId channelId,
        ChannelType channelType,
        Sku sku,
        Operation operation,
        long revision,
        CommandPriority priority,
        Set<FieldGroup> changedGroups,
        Map<String, Object> payload,
        int attempt,
        Instant issuedAt)
        implements DomainEvent {

    public enum Operation {
        UPSERT,
        INVENTORY,
        PRICE,
        END,
        RELIST
    }

    public ChannelCommand {
        changedGroups = changedGroups == null ? Set.of() : Set.copyOf(changedGroups);
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public ChannelCommand nextAttempt() {
        return new ChannelCommand(
                commandId,
                tenantId,
                channelId,
                channelType,
                sku,
                operation,
                revision,
                priority,
                changedGroups,
                payload,
                attempt + 1,
                issuedAt);
    }

    @Override
    public String eventId() {
        return commandId;
    }

    @Override
    public Instant occurredAt() {
        return issuedAt;
    }

    @Override
    public String partitionKey() {
        return Ids.partitionKey(tenantId, sku);
    }

    @Override
    public String topic() {
        return Topics.channelCommand(channelType, priority);
    }
}
