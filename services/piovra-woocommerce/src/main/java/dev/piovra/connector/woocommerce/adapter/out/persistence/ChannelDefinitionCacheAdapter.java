package dev.piovra.connector.woocommerce.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import dev.piovra.common.ErrorClass;
import dev.piovra.common.PiovraException;
import dev.piovra.connector.woocommerce.application.port.out.ChannelDefinitionCache;
import dev.piovra.model.channel.ChannelDefinition;
import dev.piovra.model.channel.ChannelType;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Repository
public class ChannelDefinitionCacheAdapter implements ChannelDefinitionCache {

    private final ChannelDefinitionCacheJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public ChannelDefinitionCacheAdapter(ChannelDefinitionCacheJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ChannelDefinition> enabledChannelsOfType(ChannelType type) {
        return jpaRepository.findByTypeAndEnabledTrue(type.name()).stream()
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
                    existing.update(definition.type().name(), definition.enabled(), payload);
                    return existing;
                })
                .orElseGet(() -> new ChannelDefinitionCacheEntity(
                        UUID.randomUUID(),
                        definition.tenantId().value(),
                        definition.channelId().value(),
                        definition.type().name(),
                        definition.enabled(),
                        payload));
        jpaRepository.save(entity);
    }

    private String write(ChannelDefinition definition) {
        try {
            return objectMapper.writeValueAsString(definition);
        } catch (JacksonException e) {
            throw new PiovraException(
                    ErrorClass.INTERNAL, "CHANNEL_CACHE_SERIALIZATION", "cannot serialize channel definition", e);
        }
    }

    private ChannelDefinition read(String payload) {
        try {
            return objectMapper.readValue(payload, ChannelDefinition.class);
        } catch (JacksonException e) {
            throw new PiovraException(
                    ErrorClass.INTERNAL, "CHANNEL_CACHE_DESERIALIZATION", "cannot deserialize channel definition", e);
        }
    }
}
