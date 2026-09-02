package dev.piovra.publication.domain;

import dev.piovra.common.Money;
import dev.piovra.common.Sku;
import dev.piovra.model.channel.ChannelDefinition;
import dev.piovra.model.channel.ChannelPolicy;
import dev.piovra.model.product.CanonicalProduct;
import dev.piovra.model.product.CanonicalVariant;
import dev.piovra.model.product.ChannelOverride;
import dev.piovra.model.product.LocalizedText;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Projects the canonical product onto a channel.
 *
 * <p>A pure function: no I/O, no state. That is deliberate — this is the class that decides what
 * the end customer will see, and it must be verifiable with golden tests that run in milliseconds.
 */
public final class ChannelProjector {

    private final Locale locale;

    public ChannelProjector(Locale locale) {
        this.locale = locale;
    }

    public DesiredListing project(CanonicalProduct product, ChannelDefinition channel, Map<Sku, Integer> availableBySku) {

        ChannelOverride override = product.overrideFor(channel.channelId()).orElse(null);
        ChannelPolicy policy = channel.policy();

        String title = text(override == null ? null : override.title(), product.title());
        String description = text(override == null ? null : override.description(), product.description());

        String categoryId = Optional.ofNullable(override == null ? null : override.categoryId())
                .or(() -> channel.resolveCategory(product.categoryPath()))
                .orElse(null);

        Map<String, String> attributes = override == null || override.attributes().isEmpty()
                ? product.attributes()
                : merge(product.attributes(), override.attributes());

        List<DesiredListing.DesiredVariant> variants = product.variants().stream()
                .map(v -> projectVariant(v, override, policy, availableBySku.getOrDefault(v.sku(), 0)))
                .toList();

        return new DesiredListing(
                product.sku(),
                product.revision(),
                title,
                description,
                product.brand(),
                categoryId,
                attributes,
                product.media(),
                variants);
    }

    private DesiredListing.DesiredVariant projectVariant(
            CanonicalVariant variant, ChannelOverride override, ChannelPolicy policy, int available) {

        Money base = override != null && override.price() != null ? override.price() : variant.price();
        Money adjusted = base.withPercentAdjustment(policy.priceAdjustmentPercent());

        return new DesiredListing.DesiredVariant(
                variant.sku(),
                variant.axisValues(),
                adjusted,
                variant.compareAtPrice(),
                policy.publishableQuantity(available),
                variant.weightGrams(),
                variant.dimensions());
    }

    private String text(LocalizedText override, LocalizedText canonical) {
        LocalizedText source = override != null && !override.isEmpty() ? override : canonical;
        return source == null ? "" : source.resolve(locale).orElse("");
    }

    private static Map<String, String> merge(Map<String, String> base, Map<String, String> overrides) {
        java.util.Map<String, String> merged = new java.util.LinkedHashMap<>(base);
        merged.putAll(overrides);
        return Map.copyOf(merged);
    }
}
