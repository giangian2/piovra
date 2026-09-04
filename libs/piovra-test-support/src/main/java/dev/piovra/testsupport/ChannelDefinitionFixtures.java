package dev.piovra.testsupport;

import java.util.Map;

import dev.piovra.common.ChannelId;
import dev.piovra.common.TenantId;
import dev.piovra.model.channel.ChannelDefinition;
import dev.piovra.model.channel.ChannelPolicy;
import dev.piovra.model.channel.ChannelType;

/** Minimal, valid {@link ChannelDefinition} instances for tests. */
public final class ChannelDefinitionFixtures {

    private ChannelDefinitionFixtures() {}

    public static ChannelDefinition channel(String channelId) {
        return channel(channelId, ChannelType.WOOCOMMERCE);
    }

    public static ChannelDefinition channel(String channelId, ChannelType type) {
        return new ChannelDefinition(
                TenantId.DEFAULT,
                ChannelId.of(channelId),
                type,
                "TEST_MARKETPLACE",
                true,
                "vault://test/" + channelId,
                ChannelPolicy.DEFAULT,
                Map.of(),
                Map.of());
    }
}
