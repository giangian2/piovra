package dev.piovra.publication.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.publication.application.port.in.FindChannelListingUseCase;
import dev.piovra.publication.application.port.out.ChannelListingRepository;
import dev.piovra.publication.domain.ChannelListing;

@Service
public class ChannelListingQueryService implements FindChannelListingUseCase {

    private final ChannelListingRepository repository;

    public ChannelListingQueryService(ChannelListingRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ChannelListing> findAllForSku(TenantId tenantId, Sku sku) {
        return repository.findAllForSku(tenantId, sku);
    }

    @Override
    public Optional<ChannelListing> find(TenantId tenantId, Sku sku, ChannelId channelId) {
        return repository.find(tenantId, sku, channelId);
    }
}
