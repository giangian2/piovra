package dev.piovra.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.piovra.catalog.domain.service.ProductFieldDiffer;
import dev.piovra.common.Money;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.model.product.CanonicalProduct;
import dev.piovra.model.product.CanonicalVariant;
import dev.piovra.model.product.Identifiers;
import dev.piovra.model.product.LocalizedText;
import dev.piovra.model.product.ProductStatus;
import dev.piovra.model.product.ProductType;

class ProductFieldDifferTest {

    private static final TenantId TENANT = TenantId.of("acme");
    private static final Sku SKU = Sku.of("TSHIRT-BASE");

    @Test
    void identical_products_produce_no_changed_fields() {
        CanonicalProduct p = product("T-shirt", "19.90");

        assertThat(ProductFieldDiffer.changedFields(p, product("T-shirt", "19.90")))
                .isEmpty();
    }

    @Test
    void a_title_change_is_reported() {
        CanonicalProduct before = product("T-shirt", "19.90");
        CanonicalProduct after = product("T-shirt premium", "19.90");

        assertThat(ProductFieldDiffer.changedFields(before, after)).containsExactly("title");
    }

    @Test
    void a_price_change_is_reported_under_variants() {
        CanonicalProduct before = product("T-shirt", "19.90");
        CanonicalProduct after = product("T-shirt", "21.90");

        assertThat(ProductFieldDiffer.changedFields(before, after)).containsExactly("variants");
    }

    @Test
    void a_status_change_is_reported() {
        CanonicalProduct before = product("T-shirt", "19.90");
        CanonicalProduct after = product("T-shirt", "19.90", ProductStatus.DISCONTINUED);

        assertThat(ProductFieldDiffer.changedFields(before, after)).containsExactly("status");
    }

    @Test
    void several_changes_are_all_reported() {
        CanonicalProduct before = product("T-shirt", "19.90");
        CanonicalProduct after = product("T-shirt premium", "21.90");

        assertThat(ProductFieldDiffer.changedFields(before, after)).containsExactlyInAnyOrder("title", "variants");
    }

    @Test
    void a_manufacturer_profile_change_is_reported() {
        CanonicalProduct before = product("T-shirt", "19.90");
        CanonicalProduct after = product("T-shirt", "19.90", "manufacturer-1");

        assertThat(ProductFieldDiffer.changedFields(before, after)).containsExactly("manufacturerProfileId");
    }

    @Test
    void resending_the_same_manufacturer_profile_is_a_noop() {
        CanonicalProduct before = product("T-shirt", "19.90", "manufacturer-1");
        CanonicalProduct after = product("T-shirt", "19.90", "manufacturer-1");

        assertThat(ProductFieldDiffer.changedFields(before, after)).isEmpty();
    }

    private static CanonicalProduct product(String title, String price) {
        return product(title, price, ProductStatus.ACTIVE, null);
    }

    private static CanonicalProduct product(String title, String price, String manufacturerProfileId) {
        return product(title, price, ProductStatus.ACTIVE, manufacturerProfileId);
    }

    private static CanonicalProduct product(String title, String price, ProductStatus status) {
        return product(title, price, status, null);
    }

    private static CanonicalProduct product(
            String title, String price, ProductStatus status, String manufacturerProfileId) {
        return new CanonicalProduct(
                TENANT,
                SKU,
                1,
                status,
                ProductType.SIMPLE,
                LocalizedText.it(title),
                LocalizedText.it("description"),
                "Acme",
                List.of("Clothing", "T-shirts"),
                Identifiers.EMPTY,
                List.of(),
                Map.of(),
                List.of(),
                List.of(CanonicalVariant.simple(SKU, Money.euro(price))),
                Map.of(),
                manufacturerProfileId,
                null,
                List.of(),
                Instant.parse("2026-09-01T00:00:00Z"));
    }
}
