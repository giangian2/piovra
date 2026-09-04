package dev.piovra.catalog.domain.service;

import java.util.Set;

import dev.piovra.events.ProductChanged;
import dev.piovra.model.product.CanonicalProduct;

/**
 * What {@link CatalogUpsertService} decided to do with an incoming product. Mirrors the shape of
 * {@code PublicationDecision} in piovra-publication: the domain decides the kind of change, the
 * application layer builds and appends the actual event.
 *
 * @param product the product as it should be persisted (revision and updatedAt already resolved)
 * @param changeType null when {@code noop} is true
 * @param noop true when nothing material changed: revision is not bumped and nothing is emitted
 */
public record UpsertPlan(
        CanonicalProduct product, ProductChanged.ChangeType changeType, Set<String> changedFields, boolean noop) {

    public static UpsertPlan noop(CanonicalProduct unchanged) {
        return new UpsertPlan(unchanged, null, Set.of(), true);
    }
}
