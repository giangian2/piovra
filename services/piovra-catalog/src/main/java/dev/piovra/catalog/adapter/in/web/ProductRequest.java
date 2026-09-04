package dev.piovra.catalog.adapter.in.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.model.product.CanonicalProduct;
import dev.piovra.model.product.CanonicalVariant;
import dev.piovra.model.product.ChannelOverride;
import dev.piovra.model.product.Identifiers;
import dev.piovra.model.product.LocalizedText;
import dev.piovra.model.product.Media;
import dev.piovra.model.product.ProductStatus;
import dev.piovra.model.product.ProductType;

/** Request body of {@code PUT /v1/products/{sku}}: everything but the identity and the bookkeeping
 * fields (revision, updatedAt), which the catalog controls (docs/03-data-model.md section 2). */
public record ProductRequest(
        ProductStatus status,
        ProductType type,
        LocalizedText title,
        LocalizedText description,
        String brand,
        List<String> categoryPath,
        Identifiers identifiers,
        List<Media> media,
        Map<String, String> attributes,
        List<String> variantAxes,
        List<CanonicalVariant> variants,
        Map<ChannelId, ChannelOverride> channelOverrides) {

    public CanonicalProduct toProduct(TenantId tenantId, Sku sku) {
        return new CanonicalProduct(
                tenantId,
                sku,
                0,
                status,
                type,
                title,
                description,
                brand,
                categoryPath,
                identifiers,
                media,
                attributes,
                variantAxes,
                variants,
                channelOverrides,
                Instant.now());
    }
}
