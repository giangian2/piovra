package dev.piovra.publication.domain;

import dev.piovra.common.Money;
import dev.piovra.common.Sku;
import dev.piovra.model.product.Dimensions;
import dev.piovra.model.product.Media;
import java.util.List;
import java.util.Map;

/**
 * What we want the channel to show: canonical product plus overrides, policy and category mapping,
 * all already resolved. It is the diff's term of comparison.
 *
 * <p>The computation that produces it ({@link ChannelProjector}) is <b>pure and deterministic</b>:
 * same input, same output, same hash. Without that property the diff would not be trustworthy and
 * golden tests would be meaningless.
 */
public record DesiredListing(
        Sku sku,
        long revision,
        String title,
        String description,
        String brand,
        String channelCategoryId,
        Map<String, String> attributes,
        List<Media> media,
        List<DesiredVariant> variants) {

    public DesiredListing {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        media = media == null ? List.of() : List.copyOf(media);
        variants = variants == null ? List.of() : List.copyOf(variants);
    }

    public record DesiredVariant(
            Sku sku,
            Map<String, String> axisValues,
            Money price,
            Money compareAtPrice,
            /** Already net of the channel buffer and of the maximum cap. */
            int quantity,
            Integer weightGrams,
            Dimensions dimensions) {

        public DesiredVariant {
            axisValues = axisValues == null ? Map.of() : Map.copyOf(axisValues);
        }
    }

    public int totalQuantity() {
        return variants.stream().mapToInt(DesiredVariant::quantity).sum();
    }
}
