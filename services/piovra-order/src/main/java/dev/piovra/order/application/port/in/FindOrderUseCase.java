package dev.piovra.order.application.port.in;

import java.util.Optional;

import dev.piovra.common.TenantId;
import dev.piovra.model.order.CanonicalOrder;

public interface FindOrderUseCase {

    Optional<CanonicalOrder> find(TenantId tenantId, String orderId);
}
