package dev.piovra.publication.application.port.out;

import java.util.List;
import java.util.Map;

import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;

/**
 * Publication's own local read-model of stock, fed by consuming {@code inventory.changed.v1} - it
 * cannot call inventory-service directly ({@code ArchitectureTest.services_do_not_call_each_other}),
 * per docs/02-services.md that is the intended integration, same as {@link ChannelDefinitionCache}.
 */
public interface InventoryCache {

    /** A SKU absent from the map is simply not there - callers already treat a missing entry as 0
     * (see {@code ChannelProjector.projectVariant}'s {@code getOrDefault}). */
    Map<Sku, Integer> availableFor(TenantId tenantId, List<Sku> skus);

    void upsert(TenantId tenantId, Sku sku, int available);
}
