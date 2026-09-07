package dev.piovra.inventory.adapter.out.kafka;

import dev.piovra.outbox.OutboxRepository;

public interface InventoryOutboxRepository extends OutboxRepository<InventoryOutboxEvent> {}
