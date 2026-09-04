package dev.piovra.events;

import java.time.Instant;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.TenantId;
import dev.piovra.model.channel.ChannelDefinition;

/**
 * A channel was registered or its configuration changed. Published on the compacted
 * {@link Topics#CHANNEL_CONFIG} topic so every downstream service (publication, connectors) keeps a
 * local read-model cache without a synchronous call back to channel-config
 * (docs/02-services.md, "channel-config").
 */
public record ChannelConfigChanged(
        String eventId, TenantId tenantId, ChannelId channelId, ChannelDefinition definition, Instant occurredAt)
        implements DomainEvent {

    public static ChannelConfigChanged of(ChannelDefinition definition) {
        return new ChannelConfigChanged(
                Ids.newId(), definition.tenantId(), definition.channelId(), definition, Instant.now());
    }

    @Override
    public String partitionKey() {
        return channelId.value();
    }

    @Override
    public String topic() {
        return Topics.CHANNEL_CONFIG;
    }
}
