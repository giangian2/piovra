package dev.piovra.publication.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.common.ErrorClass;
import dev.piovra.common.PiovraException;
import dev.piovra.common.TenantId;
import dev.piovra.model.channel.ChannelDefinition;
import dev.piovra.publication.application.port.out.ChannelDefinitionCache;

@Repository
public class ChannelDefinitionCacheAdapter implements ChannelDefinitionCache {

    private final ChannelDefinitionCacheJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public ChannelDefinitionCacheAdapter(ChannelDefinitionCacheJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ChannelDefinition> activeChannelsFor(TenantId tenantId) {
        return jpaRepository.findByTenantIdAndEnabledTrue(tenantId.value()).stream()
                .map(entity -> read(entity.payload()))
                .toList();
    }

    @Override
    public void upsert(ChannelDefinition definition) {
        String payload = write(definition);
        ChannelDefinitionCacheEntity entity = jpaRepository
                .findByTenantIdAndChannelId(
                        definition.tenantId().value(), definition.channelId().value())
                .map(existing -> {
                    existing.update(definition.enabled(), payload);
                    return existing;
                })
                .orElseGet(() -> new ChannelDefinitionCacheEntity(
                        UUID.randomUUID(),
                        definition.tenantId().value(),
                        definition.channelId().value(),
                        definition.enabled(),
                        payload));
        jpaRepository.save(entity);
    }

    private String write(ChannelDefinition definition) {
        try {
            return objectMapper.writeValueAsString(definition);
        } catch (JsonProcessingException e) {
            throw new PiovraException(
                    ErrorClass.INTERNAL, "CHANNEL_CACHE_SERIALIZATION", "cannot serialize channel definition", e);
        }
    }

    private ChannelDefinition read(String payload) {
        try {
            return objectMapper.readValue(payload, ChannelDefinition.class);
        } catch (JsonProcessingException e) {
            throw new PiovraException(
                    ErrorClass.INTERNAL, "CHANNEL_CACHE_DESERIALIZATION", "cannot deserialize channel definition", e);
        }
    }
}
