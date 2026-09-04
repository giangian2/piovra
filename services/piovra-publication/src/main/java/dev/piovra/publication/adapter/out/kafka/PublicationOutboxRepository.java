package dev.piovra.publication.adapter.out.kafka;

import dev.piovra.outbox.OutboxRepository;

public interface PublicationOutboxRepository extends OutboxRepository<PublicationOutboxEvent> {}
