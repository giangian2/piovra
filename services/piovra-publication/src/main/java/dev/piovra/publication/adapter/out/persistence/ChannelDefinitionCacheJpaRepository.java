package dev.piovra.publication.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelDefinitionCacheJpaRepository extends JpaRepository<ChannelDefinitionCacheEntity, UUID> {

    Optional<ChannelDefinitionCacheEntity> findByTenantIdAndChannelId(String tenantId, String channelId);

    List<ChannelDefinitionCacheEntity> findByTenantIdAndEnabledTrue(String tenantId);
}
