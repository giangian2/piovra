package dev.piovra.order.application.port.out;

import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;

/**
 * Order-service's own local read-model of "SKUs the catalog knows about", fed by consuming
 * {@code catalog.product.changed} - it cannot call catalog-service directly
 * ({@code ArchitectureTest.services_do_not_call_each_other}), same pattern as
 * {@code ChannelDefinitionCache}/{@code InventoryCache} in publication.
 */
public interface KnownSkuRepository {

    boolean exists(TenantId tenantId, Sku sku);

    void ensureExists(TenantId tenantId, Sku sku);
}
