package dev.piovra.publication.application.port.out;

import java.util.List;
import java.util.Optional;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.publication.domain.ChannelListing;

public interface ChannelListingRepository {

    Optional<ChannelListing> find(TenantId tenantId, Sku sku, ChannelId channelId);

    /** Every channel this SKU has a row for, published or not - the cross-channel status view. */
    List<ChannelListing> findAllForSku(TenantId tenantId, Sku sku);

    ChannelListing save(ChannelListing listing);
}
