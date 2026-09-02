package dev.piovra.model.product;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The canonical product: source of truth, independent of any marketplace
 * (docs/adr/0001-canonical-model.md).
 *
 * <p>Rule for evolving this record: only concepts common to every channel belong here. Anything
 * channel-specific goes into {@code attributes} or {@code channelOverrides}, never into a dedicated
 * field such as "ebayCategoryId".
 *
 * <p>{@code revision} is monotonic and changes ONLY when something material changes: it is what
 * lets drivers discard stale commands.
 */
public record CanonicalProduct(
        TenantId tenantId,
        Sku sku,
        long revision,
        ProductStatus status,
        ProductType type,
        LocalizedText title,
        LocalizedText description,
        String brand,
        List<String> categoryPath,
        Identifiers identifiers,
        List<Media> media,
        Map<String, String> attributes,
        /** Names of the variation axes, in order: ["size", "color"]. */
        List<String> variantAxes,
        List<CanonicalVariant> variants,
        /** Per-channel overrides: avoids duplicating the product just to publish a different price. */
        Map<ChannelId, ChannelOverride> channelOverrides,
        Instant updatedAt) {

    public CanonicalProduct {
        categoryPath = categoryPath == null ? List.of() : List.copyOf(categoryPath);
        media = media == null ? List.of() : List.copyOf(media);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        variantAxes = variantAxes == null ? List.of() : List.copyOf(variantAxes);
        variants = variants == null ? List.of() : List.copyOf(variants);
        channelOverrides = channelOverrides == null ? Map.of() : Map.copyOf(channelOverrides);
        if (variants.isEmpty()) {
            throw new IllegalArgumentException("product without variants: " + sku);
        }
    }

    public boolean isPublishable() {
        return status == ProductStatus.ACTIVE;
    }

    public Optional<CanonicalVariant> variant(Sku variantSku) {
        return variants.stream().filter(v -> v.sku().equals(variantSku)).findFirst();
    }

    public Optional<ChannelOverride> overrideFor(ChannelId channelId) {
        return Optional.ofNullable(channelOverrides.get(channelId));
    }

    public Optional<Media> mainImage() {
        return media.stream().filter(m -> m.role() == MediaRole.MAIN).findFirst();
    }
}
