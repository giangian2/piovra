package dev.piovra.connector.woocommerce.application.port.out;

import java.util.List;

import dev.piovra.model.channel.ChannelDefinition;
import dev.piovra.model.channel.ChannelType;

/**
 * The connector's own local read-model of channel configuration, fed by consuming the compacted
 * {@code channel.config.v1} topic. It cannot call channel-config directly
 * ({@code ArchitectureTest.services_do_not_call_each_other}), and per docs/02-services.md that is
 * the intended integration: every downstream component keeps its own cache.
 */
public interface ChannelDefinitionCache {

    /** Enabled channels only, across every tenant: a disabled channel is never polled. */
    List<ChannelDefinition> enabledChannelsOfType(ChannelType type);

    void upsert(ChannelDefinition definition);
}
