package dev.piovra.order.application.port.out;

import java.util.Optional;

import dev.piovra.common.ChannelId;
import dev.piovra.common.TenantId;
import dev.piovra.model.order.CanonicalOrder;

public interface OrderRepository {

    /** The dedup lookup: {@code (tenantId, channelId, channelOrderId)} is the natural key. */
    Optional<CanonicalOrder> findByChannelOrderId(TenantId tenantId, ChannelId channelId, String channelOrderId);

    Optional<CanonicalOrder> findByOrderId(TenantId tenantId, String orderId);

    CanonicalOrder save(CanonicalOrder order);
}
