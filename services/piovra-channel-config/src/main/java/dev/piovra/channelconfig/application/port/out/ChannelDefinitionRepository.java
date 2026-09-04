package dev.piovra.channelconfig.application.port.out;

import java.util.List;
import java.util.Optional;

import dev.piovra.common.ChannelId;
import dev.piovra.common.TenantId;
import dev.piovra.model.channel.ChannelDefinition;

public interface ChannelDefinitionRepository {

    Optional<ChannelDefinition> findByChannelId(TenantId tenantId, ChannelId channelId);

    List<ChannelDefinition> findByTenant(TenantId tenantId);

    ChannelDefinition save(ChannelDefinition definition);
}
