package dev.piovra.testsupport;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import dev.piovra.common.Money;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.model.product.CanonicalProduct;
import dev.piovra.model.product.CanonicalVariant;
import dev.piovra.model.product.Identifiers;
import dev.piovra.model.product.LocalizedText;
import dev.piovra.model.product.ProductStatus;
import dev.piovra.model.product.ProductType;

/** Minimal, valid {@link CanonicalProduct} instances for tests, avoiding repeated boilerplate. */
public final class CanonicalProductFixtures {

    private CanonicalProductFixtures() {}

    public static CanonicalProduct simpleProduct(String sku) {
        return simpleProduct(sku, Money.euro("19.90"));
    }

    public static CanonicalProduct simpleProduct(String sku, Money price) {
        Sku canonicalSku = Sku.of(sku);
        return new CanonicalProduct(
                TenantId.DEFAULT,
                canonicalSku,
                1,
                ProductStatus.ACTIVE,
                ProductType.SIMPLE,
                LocalizedText.it("Test product " + sku),
                LocalizedText.it("Description of " + sku),
                "Acme",
                List.of("Test"),
                Identifiers.EMPTY,
                List.of(),
                Map.of(),
                List.of(),
                List.of(CanonicalVariant.simple(canonicalSku, price)),
                Map.of(),
                Instant.now());
    }
}
