package dev.piovra.channelconfig.application.port.in;

import dev.piovra.model.channel.ChannelDefinition;

/** Registers a channel or updates its configuration. */
public interface RegisterChannelUseCase {

    ChannelDefinition register(ChannelDefinition definition);
}
