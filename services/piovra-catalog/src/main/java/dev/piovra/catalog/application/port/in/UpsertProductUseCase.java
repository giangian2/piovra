package dev.piovra.catalog.application.port.in;

import java.util.Optional;

import dev.piovra.common.TenantId;
import dev.piovra.events.ProductChanged;
import dev.piovra.model.product.CanonicalProduct;

public interface UpsertProductUseCase {

    /** Empty when the incoming product is identical to what is already stored (no-op). */
    Optional<ProductChanged> upsert(TenantId tenantId, CanonicalProduct draft);
}
