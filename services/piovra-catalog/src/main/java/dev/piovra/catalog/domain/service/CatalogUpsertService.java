package dev.piovra.catalog.domain.service;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import dev.piovra.events.ProductChanged;
import dev.piovra.model.product.CanonicalProduct;
import dev.piovra.model.product.ProductStatus;

/**
 * Decides what to do with an incoming product: create, update, discontinue, or nothing at all.
 *
 * <p>A pure function, no I/O - the catalog's equivalent of publication's {@code DiffCalculator}: same
 * input, same output, testable in milliseconds without a Spring context
 * (docs/12-development-guidelines.md section 4).
 */
public final class CatalogUpsertService {

    private CatalogUpsertService() {}

    public static UpsertPlan plan(Optional<CanonicalProduct> existing, CanonicalProduct incoming) {
        if (existing.isEmpty()) {
            return new UpsertPlan(withRevision(incoming, 1), ProductChanged.ChangeType.CREATED, Set.of("*"), false);
        }

        CanonicalProduct current = existing.get();
        Set<String> changedFields = ProductFieldDiffer.changedFields(current, incoming);
        if (changedFields.isEmpty()) {
            // The first noise filter (docs/02-services.md, "catalog-service"): nothing changed, so
            // the revision is not bumped and nothing is ever emitted.
            return UpsertPlan.noop(current);
        }

        CanonicalProduct updated = withRevision(incoming, current.revision() + 1);
        ProductChanged.ChangeType changeType = updated.status() == ProductStatus.DISCONTINUED
                ? ProductChanged.ChangeType.DISCONTINUED
                : ProductChanged.ChangeType.UPDATED;
        return new UpsertPlan(updated, changeType, changedFields, false);
    }

    private static CanonicalProduct withRevision(CanonicalProduct product, long revision) {
        return new CanonicalProduct(
                product.tenantId(),
                product.sku(),
                revision,
                product.status(),
                product.type(),
                product.title(),
                product.description(),
                product.brand(),
                product.categoryPath(),
                product.identifiers(),
                product.media(),
                product.attributes(),
                product.variantAxes(),
                product.variants(),
                product.channelOverrides(),
                Instant.now());
    }
}
