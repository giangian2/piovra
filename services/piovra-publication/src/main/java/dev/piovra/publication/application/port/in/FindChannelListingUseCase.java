package dev.piovra.publication.application.port.in;

import java.util.List;
import java.util.Optional;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.publication.domain.ChannelListing;

public interface FindChannelListingUseCase {

    /** Publication status across every channel this SKU has a row for. */
    List<ChannelListing> findAllForSku(TenantId tenantId, Sku sku);

    Optional<ChannelListing> find(TenantId tenantId, Sku sku, ChannelId channelId);
}
