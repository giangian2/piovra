package dev.piovra.catalog.domain.service;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import dev.piovra.model.product.CanonicalProduct;

/**
 * Compares two revisions of a product and reports which top-level fields actually changed - the
 * "first noise filter" (docs/02-services.md, "catalog-service"): if a feed resends identical data,
 * this is what lets the catalog bump nothing and emit nothing.
 *
 * <p>Deliberately top-level granularity (not the dotted per-locale/per-variant paths shown as an
 * example in docs/04-kafka-events.md, e.g. {@code "title.it"}): simpler to compute and to test, and
 * callers only use the field set to decide whether something changed at all, never to address a
 * single nested value.
 */
public final class ProductFieldDiffer {

    private ProductFieldDiffer() {}

    public static Set<String> changedFields(CanonicalProduct before, CanonicalProduct after) {
        Set<String> changed = new LinkedHashSet<>();
        addIfChanged(changed, "status", before.status(), after.status());
        addIfChanged(changed, "title", before.title(), after.title());
        addIfChanged(changed, "description", before.description(), after.description());
        addIfChanged(changed, "brand", before.brand(), after.brand());
        addIfChanged(changed, "categoryPath", before.categoryPath(), after.categoryPath());
        addIfChanged(changed, "identifiers", before.identifiers(), after.identifiers());
        addIfChanged(changed, "media", before.media(), after.media());
        addIfChanged(changed, "attributes", before.attributes(), after.attributes());
        addIfChanged(changed, "variantAxes", before.variantAxes(), after.variantAxes());
        addIfChanged(changed, "variants", before.variants(), after.variants());
        addIfChanged(changed, "channelOverrides", before.channelOverrides(), after.channelOverrides());
        return changed;
    }

    private static void addIfChanged(Set<String> changed, String field, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            changed.add(field);
        }
    }
}
