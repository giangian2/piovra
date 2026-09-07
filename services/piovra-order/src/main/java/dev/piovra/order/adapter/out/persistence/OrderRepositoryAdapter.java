package dev.piovra.order.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import dev.piovra.common.ChannelId;
import dev.piovra.common.TenantId;
import dev.piovra.model.order.CanonicalOrder;
import dev.piovra.order.application.port.out.OrderRepository;

@Repository
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final OrderEntityMapper mapper;

    public OrderRepositoryAdapter(OrderJpaRepository jpaRepository, OrderEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<CanonicalOrder> findByChannelOrderId(
            TenantId tenantId, ChannelId channelId, String channelOrderId) {
        return jpaRepository
                .findByTenantIdAndChannelIdAndChannelOrderId(tenantId.value(), channelId.value(), channelOrderId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<CanonicalOrder> findByOrderId(TenantId tenantId, String orderId) {
        return jpaRepository.findByTenantIdAndId(tenantId.value(), orderId).map(mapper::toDomain);
    }

    @Override
    public CanonicalOrder save(CanonicalOrder order) {
        OrderEntity entity = jpaRepository
                .findById(order.orderId())
                .map(existing -> {
                    mapper.applyTo(existing, order);
                    return existing;
                })
                .orElseGet(() -> mapper.toNewEntity(order));
        jpaRepository.save(entity);
        return order;
    }
}
