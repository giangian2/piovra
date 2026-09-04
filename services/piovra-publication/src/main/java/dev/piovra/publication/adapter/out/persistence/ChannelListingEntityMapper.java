package dev.piovra.publication.adapter.out.persistence;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.common.ChannelId;
import dev.piovra.common.ErrorClass;
import dev.piovra.common.PiovraException;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.model.channel.FieldGroup;
import dev.piovra.publication.domain.ChannelListing;
import dev.piovra.publication.domain.ListingState;

@Component
public class ChannelListingEntityMapper {

    private static final TypeReference<Map<String, String>> VARIANT_IDS_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<FieldGroup, String>> FIELD_HASHES_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public ChannelListingEntityMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ChannelListingEntity toNewEntity(ChannelListing listing) {
        return new ChannelListingEntity(
                UUID.randomUUID(),
                listing.tenantId().value(),
                listing.sku().value(),
                listing.channelId().value(),
                listing.externalId(),
                write(listing.externalVariantIds()),
                listing.state().name(),
                listing.publishedRevision(),
                write(listing.fieldHashes()),
                listing.lastCommandId(),
                listing.lastErrorCode(),
                listing.lastErrorMessage(),
                listing.lastAttemptAt(),
                listing.lastSuccessAt(),
                listing.retryCount());
    }

    public void applyTo(ChannelListingEntity entity, ChannelListing listing) {
        entity.apply(
                listing.externalId(),
                write(listing.externalVariantIds()),
                listing.state().name(),
                listing.publishedRevision(),
                write(listing.fieldHashes()),
                listing.lastCommandId(),
                listing.lastErrorCode(),
                listing.lastErrorMessage(),
                listing.lastAttemptAt(),
                listing.lastSuccessAt(),
                listing.retryCount());
    }

    public ChannelListing toDomain(ChannelListingEntity entity) {
        return new ChannelListing(
                TenantId.of(entity.tenantId()),
                Sku.of(entity.sku()),
                ChannelId.of(entity.channelId()),
                entity.externalId(),
                readMap(entity.externalVariantIds(), VARIANT_IDS_TYPE),
                ListingState.valueOf(entity.state()),
                entity.publishedRevision(),
                readMap(entity.fieldHashes(), FIELD_HASHES_TYPE),
                entity.lastCommandId(),
                entity.lastErrorCode(),
                entity.lastErrorMessage(),
                entity.retryCount(),
                entity.lastAttemptAt(),
                entity.lastSuccessAt());
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new PiovraException(
                    ErrorClass.INTERNAL, "CHANNEL_LISTING_SERIALIZATION", "cannot serialize channel listing", e);
        }
    }

    private <T> T readMap(String json, TypeReference<T> type) {
        if (json == null) {
            throw new PiovraException(ErrorClass.INTERNAL, "CHANNEL_LISTING_DESERIALIZATION", "missing map payload");
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new PiovraException(
                    ErrorClass.INTERNAL, "CHANNEL_LISTING_DESERIALIZATION", "cannot deserialize channel listing", e);
        }
    }
}
