package dev.piovra.model.channel;

import java.util.Map;
import java.util.Optional;

import dev.piovra.common.ChannelId;
import dev.piovra.common.TenantId;

/**
 * A channel as the downstream services see it. Credentials are NOT here: only the Vault reference,
 * resolved by the connector at call time.
 */
public record ChannelDefinition(
        TenantId tenantId,
        ChannelId channelId,
        ChannelType type,
        String marketplaceCode,
        boolean enabled,
        String credentialsRef,
        ChannelPolicy policy,
        /** Canonical categoryPath (joined with "/") -> marketplace category id. */
        Map<String, String> categoryMapping,
        Map<String, String> settings) {

    public ChannelDefinition {
        policy = policy == null ? ChannelPolicy.DEFAULT : policy;
        categoryMapping = categoryMapping == null ? Map.of() : Map.copyOf(categoryMapping);
        settings = settings == null ? Map.of() : Map.copyOf(settings);
    }

    public Optional<String> resolveCategory(java.util.List<String> categoryPath) {
        return Optional.ofNullable(categoryMapping.get(String.join("/", categoryPath)));
    }
}
