package dev.piovra.publication.domain;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.model.channel.FieldGroup;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * The memory of what has been published on a channel for a product. This is the heart of the
 * upsert: without this state, every feed would resend everything to the marketplace
 * (docs/03-data-model.md, docs/06-publish-flow.md).
 *
 * @param externalId the listing id on the marketplace. It must be stored as soon as the marketplace
 *     returns it: losing it means creating a duplicate on the next retry.
 * @param publishedRevision canonical revision of the last successful publish
 * @param fieldHashes per-field-group hashes of the last published payload. Comparing them with the
 *     desired hashes is the entire diff logic.
 */
public record ChannelListing(
        TenantId tenantId,
        Sku sku,
        ChannelId channelId,
        String externalId,
        Map<String, String> externalVariantIds,
        ListingState state,
        long publishedRevision,
        Map<FieldGroup, String> fieldHashes,
        String lastCommandId,
        String lastErrorCode,
        String lastErrorMessage,
        int retryCount,
        Instant lastAttemptAt,
        Instant lastSuccessAt) {

    public ChannelListing {
        externalVariantIds = externalVariantIds == null ? Map.of() : Map.copyOf(externalVariantIds);
        fieldHashes = fieldHashes == null ? Map.of() : Map.copyOf(fieldHashes);
    }

    public static ChannelListing notListed(TenantId tenant, Sku sku, ChannelId channelId) {
        return new ChannelListing(
                tenant, sku, channelId, null, Map.of(), ListingState.NOT_LISTED, 0L, Map.of(), null, null, null, 0,
                null, null);
    }

    public boolean isPublished() {
        return externalId != null && state == ListingState.LISTED;
    }

    /**
     * A command whose revision is not above the published one is stale. This is the mechanism that
     * makes retries and residual reordering harmless (docs/adr/0002-kafka-sku-key.md).
     */
    public boolean isStale(long incomingRevision) {
        return incomingRevision < publishedRevision;
    }

    public Optional<String> externalIdOptional() {
        return Optional.ofNullable(externalId);
    }

    public ChannelListing markPending(String commandId, Instant now) {
        return new ChannelListing(
                tenantId, sku, channelId, externalId, externalVariantIds, ListingState.PENDING, publishedRevision,
                fieldHashes, commandId, lastErrorCode, lastErrorMessage, retryCount, now, lastSuccessAt);
    }

    public ChannelListing markPublished(
            String newExternalId,
            Map<String, String> variantIds,
            long revision,
            Map<FieldGroup, String> newHashes,
            Instant now) {
        return new ChannelListing(
                tenantId,
                sku,
                channelId,
                newExternalId != null ? newExternalId : externalId,
                variantIds.isEmpty() ? externalVariantIds : variantIds,
                ListingState.LISTED,
                revision,
                newHashes,
                lastCommandId,
                null,
                null,
                0,
                now,
                now);
    }

    public ChannelListing markError(String code, String message, Instant now) {
        return new ChannelListing(
                tenantId, sku, channelId, externalId, externalVariantIds, ListingState.ERROR, publishedRevision,
                fieldHashes, lastCommandId, code, message, retryCount + 1, now, lastSuccessAt);
    }

    public ChannelListing markBlocked(String reason, Instant now) {
        return new ChannelListing(
                tenantId, sku, channelId, externalId, externalVariantIds, ListingState.BLOCKED, publishedRevision,
                fieldHashes, lastCommandId, "BLOCKED_BY_RULE", reason, retryCount, now, lastSuccessAt);
    }

    /** Clears the hashes so the next diff produces a full publish (forced resync). */
    public ChannelListing forgetSnapshot() {
        return new ChannelListing(
                tenantId, sku, channelId, externalId, externalVariantIds, state, publishedRevision, Map.of(),
                lastCommandId, lastErrorCode, lastErrorMessage, retryCount, lastAttemptAt, lastSuccessAt);
    }
}
