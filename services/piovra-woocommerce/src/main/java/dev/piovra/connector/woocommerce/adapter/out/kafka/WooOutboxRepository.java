package dev.piovra.connector.woocommerce.adapter.out.kafka;

import dev.piovra.outbox.OutboxRepository;

public interface WooOutboxRepository extends OutboxRepository<WooOutboxEvent> {}
