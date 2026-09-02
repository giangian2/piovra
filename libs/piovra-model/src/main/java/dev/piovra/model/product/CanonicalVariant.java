package dev.piovra.model.product;

import dev.piovra.common.Money;
import dev.piovra.common.Sku;
import java.util.List;
import java.util.Map;

/**
 * The unit actually being sold. Price and stock live here: a product without variants has a single
 * variant sharing the parent SKU.
 */
public record CanonicalVariant(
        Sku sku,
        Identifiers identifiers,
        /** Values of the variation axes: {"size": "M", "color": "Red"}. Empty for simple products. */
        Map<String, String> axisValues,
        Money price,
        Money compareAtPrice,
        Integer weightGrams,
        Dimensions dimensions,
        List<Media> media,
        Map<String, String> attributes) {

    public CanonicalVariant {
        axisValues = axisValues == null ? Map.of() : Map.copyOf(axisValues);
        media = media == null ? List.of() : List.copyOf(media);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static CanonicalVariant simple(Sku sku, Money price) {
        return new CanonicalVariant(sku, Identifiers.EMPTY, Map.of(), price, null, null, null, List.of(), Map.of());
    }

    public CanonicalVariant withPrice(Money newPrice) {
        return new CanonicalVariant(
                sku, identifiers, axisValues, newPrice, compareAtPrice, weightGrams, dimensions, media, attributes);
    }
}
