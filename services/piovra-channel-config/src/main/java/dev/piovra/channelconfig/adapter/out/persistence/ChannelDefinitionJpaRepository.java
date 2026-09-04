package dev.piovra.channelconfig.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelDefinitionJpaRepository extends JpaRepository<ChannelDefinitionEntity, UUID> {

    Optional<ChannelDefinitionEntity> findByTenantIdAndChannelId(String tenantId, String channelId);

    List<ChannelDefinitionEntity> findByTenantId(String tenantId);
}
