package dev.piovra.order.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, String> {

    Optional<OrderEntity> findByTenantIdAndChannelIdAndChannelOrderId(
            String tenantId, String channelId, String channelOrderId);

    Optional<OrderEntity> findByTenantIdAndId(String tenantId, String id);
}
