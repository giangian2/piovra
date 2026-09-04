package dev.piovra.publication.application.port.out;

import java.util.List;

import dev.piovra.common.TenantId;
import dev.piovra.model.channel.ChannelDefinition;

/**
 * Publication's own local read-model of channel configuration, fed by consuming the compacted
 * {@code channel.config.v1} topic - it cannot call channel-config directly
 * ({@code ArchitectureTest.services_do_not_call_each_other}), and per docs/02-services.md that is
 * the intended integration: every downstream service keeps its own cache.
 */
public interface ChannelDefinitionCache {

    /** Enabled channels only: a disabled channel is never a publish target. */
    List<ChannelDefinition> activeChannelsFor(TenantId tenantId);

    void upsert(ChannelDefinition definition);
}
