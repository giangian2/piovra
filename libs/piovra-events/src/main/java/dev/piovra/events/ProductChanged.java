package dev.piovra.events;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import dev.piovra.common.Ids;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.model.product.CanonicalProduct;

/**
 * The catalog registered a real change (if nothing changed, this event is never born).
 *
 * <p>It is a "fat event": it carries the full state <b>and</b> the list of modified fields. The
 * full state saves every consumer a call back to the catalog; the field list lets the publication
 * service choose the cheapest command.
 */
public record ProductChanged(
        String eventId,
        TenantId tenantId,
        Sku sku,
        long revision,
        ChangeType changeType,
        /** Paths of the modified fields, e.g. ["price", "title.it", "media"]. */
        Set<String> changedFields,
        CanonicalProduct product,
        Instant occurredAt)
        implements DomainEvent {

    public enum ChangeType {
        CREATED,
        UPDATED,
        DISCONTINUED
    }

    public ProductChanged {
        changedFields = changedFields == null ? Set.of() : Set.copyOf(changedFields);
    }

    public static ProductChanged created(CanonicalProduct product) {
        return new ProductChanged(
                Ids.newId(),
                product.tenantId(),
                product.sku(),
                product.revision(),
                ChangeType.CREATED,
                Set.of("*"),
                product,
                Instant.now());
    }

    public static ProductChanged updated(CanonicalProduct product, Set<String> changedFields) {
        return new ProductChanged(
                Ids.newId(),
                product.tenantId(),
                product.sku(),
                product.revision(),
                ChangeType.UPDATED,
                changedFields,
                product,
                Instant.now());
    }

    public List<Sku> variantSkus() {
        return product.variants().stream().map(v -> v.sku()).toList();
    }

    @Override
    public String partitionKey() {
        return Ids.partitionKey(tenantId, sku);
    }

    @Override
    public String topic() {
        return Topics.CATALOG_PRODUCT_CHANGED;
    }
}
