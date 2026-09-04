package dev.piovra.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Every module's concrete outbox repository extends this with its own entity type, e.g.
 * {@code interface CatalogOutboxRepository extends OutboxRepository<CatalogOutboxEvent> {}}.
 */
public interface OutboxRepository<T extends OutboxEntity> extends JpaRepository<T, String> {

    List<T> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
