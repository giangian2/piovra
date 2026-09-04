package dev.piovra.channelconfig.application.port.in;

import java.util.List;
import java.util.Optional;

import dev.piovra.common.ChannelId;
import dev.piovra.common.TenantId;
import dev.piovra.model.channel.ChannelDefinition;

public interface FindChannelUseCase {

    Optional<ChannelDefinition> find(TenantId tenantId, ChannelId channelId);

    List<ChannelDefinition> findAll(TenantId tenantId);
}
