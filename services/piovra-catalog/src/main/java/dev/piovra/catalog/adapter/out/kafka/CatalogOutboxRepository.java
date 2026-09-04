package dev.piovra.catalog.adapter.out.kafka;

import dev.piovra.outbox.OutboxRepository;

public interface CatalogOutboxRepository extends OutboxRepository<CatalogOutboxEvent> {}
