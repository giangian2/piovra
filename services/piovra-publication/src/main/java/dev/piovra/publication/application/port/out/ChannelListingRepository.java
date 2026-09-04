package dev.piovra.publication.application.port.out;

import java.util.Optional;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.publication.domain.ChannelListing;

public interface ChannelListingRepository {

    Optional<ChannelListing> find(TenantId tenantId, Sku sku, ChannelId channelId);

    ChannelListing save(ChannelListing listing);
}
