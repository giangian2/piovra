package dev.piovra.catalog.application.port.out;

import java.util.Optional;

import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.model.product.CanonicalProduct;

public interface ProductRepository {

    Optional<CanonicalProduct> findBySku(TenantId tenantId, Sku sku);

    CanonicalProduct save(CanonicalProduct product);
}
