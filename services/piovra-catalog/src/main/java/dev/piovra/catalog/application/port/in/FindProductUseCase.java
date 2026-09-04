package dev.piovra.catalog.application.port.in;

import java.util.Optional;

import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.model.product.CanonicalProduct;

public interface FindProductUseCase {

    Optional<CanonicalProduct> find(TenantId tenantId, Sku sku);
}
