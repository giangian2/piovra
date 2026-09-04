package dev.piovra.publication.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.publication.application.port.out.ChannelListingRepository;
import dev.piovra.publication.domain.ChannelListing;

@Repository
public class ChannelListingRepositoryAdapter implements ChannelListingRepository {

    private final ChannelListingJpaRepository jpaRepository;
    private final ChannelListingEntityMapper mapper;

    public ChannelListingRepositoryAdapter(
            ChannelListingJpaRepository jpaRepository, ChannelListingEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<ChannelListing> find(TenantId tenantId, Sku sku, ChannelId channelId) {
        return jpaRepository
                .findByTenantIdAndSkuAndChannelId(tenantId.value(), sku.value(), channelId.value())
                .map(mapper::toDomain);
    }

    @Override
    public ChannelListing save(ChannelListing listing) {
        ChannelListingEntity entity = jpaRepository
                .findByTenantIdAndSkuAndChannelId(
                        listing.tenantId().value(),
                        listing.sku().value(),
                        listing.channelId().value())
                .map(existing -> {
                    mapper.applyTo(existing, listing);
                    return existing;
                })
                .orElseGet(() -> mapper.toNewEntity(listing));
        jpaRepository.save(entity);
        return listing;
    }
}
