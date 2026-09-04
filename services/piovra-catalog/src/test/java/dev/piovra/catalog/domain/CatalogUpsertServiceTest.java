package dev.piovra.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.piovra.catalog.domain.service.CatalogUpsertService;
import dev.piovra.catalog.domain.service.UpsertPlan;
import dev.piovra.common.Money;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.events.ProductChanged;
import dev.piovra.model.product.CanonicalProduct;
import dev.piovra.model.product.CanonicalVariant;
import dev.piovra.model.product.Identifiers;
import dev.piovra.model.product.LocalizedText;
import dev.piovra.model.product.ProductStatus;
import dev.piovra.model.product.ProductType;

/**
 * Mirrors publication's {@code DiffCalculatorTest} style: the plan is what decides whether a
 * marketplace call ever happens downstream, so every edge case is worth a dedicated case.
 */
class CatalogUpsertServiceTest {

    private static final TenantId TENANT = TenantId.of("acme");
    private static final Sku SKU = Sku.of("TSHIRT-BASE");

    @Test
    void a_brand_new_product_is_created_at_revision_one() {
        UpsertPlan plan = CatalogUpsertService.plan(Optional.empty(), product("T-shirt", "19.90"));

        assertThat(plan.noop()).isFalse();
        assertThat(plan.changeType()).isEqualTo(ProductChanged.ChangeType.CREATED);
        assertThat(plan.product().revision()).isEqualTo(1);
    }

    @Test
    void resubmitting_an_identical_product_is_a_noop() {
        CanonicalProduct existing = product("T-shirt", "19.90");

        UpsertPlan plan = CatalogUpsertService.plan(Optional.of(existing), product("T-shirt", "19.90"));

        assertThat(plan.noop()).isTrue();
        assertThat(plan.changedFields()).isEmpty();
        assertThat(plan.product().revision()).isEqualTo(existing.revision());
    }

    @Test
    void a_single_field_change_bumps_the_revision_by_one() {
        CanonicalProduct existing = product("T-shirt", "19.90");

        UpsertPlan plan = CatalogUpsertService.plan(Optional.of(existing), product("T-shirt premium", "19.90"));

        assertThat(plan.noop()).isFalse();
        assertThat(plan.changeType()).isEqualTo(ProductChanged.ChangeType.UPDATED);
        assertThat(plan.changedFields()).containsExactly("title");
        assertThat(plan.product().revision()).isEqualTo(existing.revision() + 1);
    }

    @Test
    void moving_to_discontinued_is_reported_as_a_discontinuation() {
        CanonicalProduct existing = product("T-shirt", "19.90");
        CanonicalProduct incoming = product("T-shirt", "19.90", ProductStatus.DISCONTINUED);

        UpsertPlan plan = CatalogUpsertService.plan(Optional.of(existing), incoming);

        assertThat(plan.changeType()).isEqualTo(ProductChanged.ChangeType.DISCONTINUED);
        assertThat(plan.product().revision()).isEqualTo(existing.revision() + 1);
    }

    @Test
    void revision_never_moves_backwards_across_repeated_updates() {
        CanonicalProduct v1 = product("T-shirt", "19.90");
        UpsertPlan firstUpdate = CatalogUpsertService.plan(Optional.of(v1), product("T-shirt", "21.90"));
        UpsertPlan secondUpdate =
                CatalogUpsertService.plan(Optional.of(firstUpdate.product()), product("T-shirt", "23.90"));

        assertThat(firstUpdate.product().revision()).isEqualTo(2);
        assertThat(secondUpdate.product().revision()).isEqualTo(3);
    }

    private static CanonicalProduct product(String title, String price) {
        return product(title, price, ProductStatus.ACTIVE);
    }

    private static CanonicalProduct product(String title, String price, ProductStatus status) {
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
                Instant.parse("2026-09-01T00:00:00Z"));
    }
}
