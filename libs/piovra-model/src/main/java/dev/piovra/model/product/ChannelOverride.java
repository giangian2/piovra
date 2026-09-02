package dev.piovra.model.product;

import java.util.Map;

import dev.piovra.common.Money;

/**
 * A per-channel override. A null field means "use the canonical value": absence must be
 * distinguishable from an empty value, which is why these are objects rather than primitives.
 */
public record ChannelOverride(
        LocalizedText title,
        LocalizedText description,
        Money price,
        /** Overrides the automatically mapped category. */
        String categoryId,
        Map<String, String> attributes,
        /** When false, the product is not published on this channel even though it is active. */
        Boolean enabled) {

    public ChannelOverride {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static ChannelOverride disabled() {
        return new ChannelOverride(null, null, null, null, Map.of(), false);
    }
}
