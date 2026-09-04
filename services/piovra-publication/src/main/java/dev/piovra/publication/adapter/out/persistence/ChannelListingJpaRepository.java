package dev.piovra.publication.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelListingJpaRepository extends JpaRepository<ChannelListingEntity, UUID> {

    Optional<ChannelListingEntity> findByTenantIdAndSkuAndChannelId(String tenantId, String sku, String channelId);
}
