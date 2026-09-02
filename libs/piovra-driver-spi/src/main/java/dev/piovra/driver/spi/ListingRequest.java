package dev.piovra.driver.spi;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import dev.piovra.model.channel.FieldGroup;
import dev.piovra.model.product.CanonicalProduct;

/**
 * A publish request. It carries the product already projected onto the channel (overrides and
 * policy applied by the publication service).
 *
 * @param externalId the listing id when it already exists; null means "create it"
 * @param externalVariantIds map of canonical SKU -> variant id on the marketplace
 * @param changedGroups the field groups that actually changed. The driver may use them to pick the
 *     cheapest call; when all of them are present, it is a full publish.
 * @param channelCategoryId marketplace category already resolved by the mapping
 * @param commandId idempotency key: the driver must not execute the same commandId twice
 */
public record ListingRequest(
        CanonicalProduct product,
        String externalId,
        Map<String, String> externalVariantIds,
        Set<FieldGroup> changedGroups,
        String channelCategoryId,
        Map<String, Integer> availableQuantities,
        String commandId,
        long revision) {

    public ListingRequest {
        externalVariantIds = externalVariantIds == null ? Map.of() : Map.copyOf(externalVariantIds);
        changedGroups = changedGroups == null ? Set.of() : Set.copyOf(changedGroups);
        availableQuantities = availableQuantities == null ? Map.of() : Map.copyOf(availableQuantities);
    }

    public boolean isCreate() {
        return externalId == null || externalId.isBlank();
    }

    public Optional<String> externalVariantId(String canonicalSku) {
        return Optional.ofNullable(externalVariantIds.get(canonicalSku));
    }
}
