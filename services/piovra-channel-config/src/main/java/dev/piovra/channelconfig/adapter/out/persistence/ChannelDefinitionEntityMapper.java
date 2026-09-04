package dev.piovra.channelconfig.adapter.out.persistence;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.common.ErrorClass;
import dev.piovra.common.PiovraException;
import dev.piovra.model.channel.ChannelDefinition;

@Component
public class ChannelDefinitionEntityMapper {

    private final ObjectMapper objectMapper;

    public ChannelDefinitionEntityMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ChannelDefinitionEntity toNewEntity(ChannelDefinition definition) {
        return new ChannelDefinitionEntity(
                UUID.randomUUID(),
                definition.tenantId().value(),
                definition.channelId().value(),
                definition.enabled(),
                write(definition));
    }

    public void applyTo(ChannelDefinitionEntity entity, ChannelDefinition definition) {
        entity.updatePayload(definition.enabled(), write(definition));
    }

    public ChannelDefinition toDomain(ChannelDefinitionEntity entity) {
        return read(entity.payload());
    }

    private String write(ChannelDefinition definition) {
        try {
            return objectMapper.writeValueAsString(definition);
        } catch (JsonProcessingException e) {
            throw new PiovraException(
                    ErrorClass.INTERNAL, "CHANNEL_CONFIG_SERIALIZATION", "cannot serialize channel definition", e);
        }
    }

    private ChannelDefinition read(String payload) {
        try {
            return objectMapper.readValue(payload, ChannelDefinition.class);
        } catch (JsonProcessingException e) {
            throw new PiovraException(
                    ErrorClass.INTERNAL, "CHANNEL_CONFIG_DESERIALIZATION", "cannot deserialize channel definition", e);
        }
    }
}
