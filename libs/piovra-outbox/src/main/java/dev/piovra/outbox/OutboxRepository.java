package dev.piovra.outbox;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Every module's concrete outbox repository extends this with its own entity type, e.g.
 * {@code interface CatalogOutboxRepository extends OutboxRepository<CatalogOutboxEvent> {}}.
 */
public interface OutboxRepository<T extends OutboxEntity> extends JpaRepository<T, String> {

    /**
     * Rows ready to (re)publish now: {@code PENDING} and either never failed ({@code nextRetryAt} is
     * null) or the backoff window has elapsed. A derived query name cannot express the OR, hence JPQL.
     */
    @Query("select e from #{#entityName} e where e.status = :status "
            + "and (e.nextRetryAt is null or e.nextRetryAt <= :now) order by e.createdAt asc")
    List<T> findReadyToPublish(@Param("status") OutboxStatus status, @Param("now") Instant now, Pageable pageable);
}
