package dev.piovra.channelconfig.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import dev.piovra.channelconfig.application.port.out.ChannelDefinitionRepository;
import dev.piovra.common.ChannelId;
import dev.piovra.common.TenantId;
import dev.piovra.model.channel.ChannelDefinition;

@Repository
public class ChannelDefinitionRepositoryAdapter implements ChannelDefinitionRepository {

    private final ChannelDefinitionJpaRepository jpaRepository;
    private final ChannelDefinitionEntityMapper mapper;

    public ChannelDefinitionRepositoryAdapter(
            ChannelDefinitionJpaRepository jpaRepository, ChannelDefinitionEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<ChannelDefinition> findByChannelId(TenantId tenantId, ChannelId channelId) {
        return jpaRepository
                .findByTenantIdAndChannelId(tenantId.value(), channelId.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<ChannelDefinition> findByTenant(TenantId tenantId) {
        return jpaRepository.findByTenantId(tenantId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public ChannelDefinition save(ChannelDefinition definition) {
        ChannelDefinitionEntity entity = jpaRepository
                .findByTenantIdAndChannelId(
                        definition.tenantId().value(), definition.channelId().value())
                .map(existing -> {
                    mapper.applyTo(existing, definition);
                    return existing;
                })
                .orElseGet(() -> mapper.toNewEntity(definition));
        jpaRepository.save(entity);
        return definition;
    }
}
