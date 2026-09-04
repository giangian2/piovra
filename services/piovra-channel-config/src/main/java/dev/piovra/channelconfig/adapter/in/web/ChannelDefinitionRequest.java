package dev.piovra.channelconfig.adapter.in.web;

import java.util.Map;

import dev.piovra.common.ChannelId;
import dev.piovra.common.TenantId;
import dev.piovra.model.channel.ChannelDefinition;
import dev.piovra.model.channel.ChannelPolicy;
import dev.piovra.model.channel.ChannelType;

/** Request body of {@code PUT /v1/channels/{channelId}}: everything but the identity, which comes
 * from the path and the tenant header. */
public record ChannelDefinitionRequest(
        ChannelType type,
        String marketplaceCode,
        boolean enabled,
        String credentialsRef,
        ChannelPolicy policy,
        Map<String, String> categoryMapping,
        Map<String, String> settings) {

    public ChannelDefinition toDefinition(TenantId tenantId, ChannelId channelId) {
        return new ChannelDefinition(
                tenantId, channelId, type, marketplaceCode, enabled, credentialsRef, policy, categoryMapping, settings);
    }
}
