package dev.piovra.order.adapter.out.kafka;

import dev.piovra.outbox.OutboxRepository;

public interface OrderOutboxRepository extends OutboxRepository<OrderOutboxEvent> {}
