package dev.piovra.connector.woocommerce.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import dev.piovra.common.ChannelId;
import dev.piovra.common.TenantId;
import dev.piovra.connector.woocommerce.application.port.out.PollCursorStore;
import dev.piovra.connector.woocommerce.application.port.out.PollKind;
import dev.piovra.connector.woocommerce.application.port.out.PollLease;
import dev.piovra.connector.woocommerce.config.ConnectorProperties;

/**
 * Plain JDBC: the table carries no behaviour, and the lease needs {@code FOR UPDATE SKIP LOCKED},
 * which is not a derived query.
 */
@Repository
public class PollCursorStoreAdapter implements PollCursorStore {

    private final JdbcTemplate jdbcTemplate;
    private final String instanceId;

    public PollCursorStoreAdapter(JdbcTemplate jdbcTemplate, ConnectorProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.instanceId = properties.instanceId();
    }

    @Override
    @Transactional
    public void ensureExists(TenantId tenantId, ChannelId channelId, PollKind kind, Instant startAt) {
        jdbcTemplate.update("""
                INSERT INTO connector_woocommerce.poll_cursor
                       (tenant_id, channel_id, poll_kind, cursor_at, updated_at)
                VALUES (?, ?, ?, ?, now())
                ON CONFLICT (tenant_id, channel_id, poll_kind) DO NOTHING
                """, tenantId.value(), channelId.value(), kind.name(), Timestamp.from(startAt));
    }

    /**
     * {@code SKIP LOCKED} settles the instantaneous race between replicas reaching for the same
     * channel; {@code leased_until} is the logical lease that outlives this transaction and covers
     * the HTTP round trip that follows it.
     */
    @Override
    @Transactional
    public Optional<PollLease> acquire(TenantId tenantId, ChannelId channelId, PollKind kind, Duration lease) {
        List<PollLease> claimed = jdbcTemplate.query(
                """
                SELECT cursor_at, page_cursor FROM connector_woocommerce.poll_cursor
                 WHERE tenant_id = ? AND channel_id = ? AND poll_kind = ?
                   AND (leased_until IS NULL OR leased_until < now())
                 FOR UPDATE SKIP LOCKED
                """,
                (rs, rowNum) -> new PollLease(
                        tenantId,
                        channelId,
                        kind,
                        rs.getTimestamp("cursor_at").toInstant(),
                        rs.getString("page_cursor")),
                tenantId.value(),
                channelId.value(),
                kind.name());

        if (claimed.isEmpty()) {
            return Optional.empty();
        }
        markLeased(tenantId, channelId, kind, lease);
        return Optional.of(claimed.getFirst());
    }

    /**
     * Joins the caller's transaction on purpose: the outbox rows for the page and this cursor move
     * commit together or not at all (docs/12-development-guidelines.md section 5.4).
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void advance(PollLease lease, Instant cursorAt, String pageCursor, Duration leaseExtension) {
        jdbcTemplate.update(
                """
                UPDATE connector_woocommerce.poll_cursor
                   SET cursor_at = ?, page_cursor = ?, leased_until = now() + ?::interval, updated_at = now()
                 WHERE tenant_id = ? AND channel_id = ? AND poll_kind = ?
                """,
                Timestamp.from(cursorAt),
                pageCursor,
                intervalOf(leaseExtension),
                lease.tenantId().value(),
                lease.channelId().value(),
                lease.kind().name());
    }

    @Override
    @Transactional
    public void release(PollLease lease, String errorCode) {
        jdbcTemplate.update(
                """
                UPDATE connector_woocommerce.poll_cursor
                   SET leased_until = NULL,
                       leased_by = NULL,
                       last_error = ?,
                       last_success_at = CASE WHEN ?::text IS NULL THEN now() ELSE last_success_at END,
                       consecutive_failures = CASE WHEN ?::text IS NULL THEN 0 ELSE consecutive_failures + 1 END,
                       updated_at = now()
                 WHERE tenant_id = ? AND channel_id = ? AND poll_kind = ?
                """,
                errorCode,
                errorCode,
                errorCode,
                lease.tenantId().value(),
                lease.channelId().value(),
                lease.kind().name());
    }

    private void markLeased(TenantId tenantId, ChannelId channelId, PollKind kind, Duration lease) {
        jdbcTemplate.update("""
                UPDATE connector_woocommerce.poll_cursor
                   SET leased_until = now() + ?::interval, leased_by = ?, updated_at = now()
                 WHERE tenant_id = ? AND channel_id = ? AND poll_kind = ?
                """, intervalOf(lease), instanceId, tenantId.value(), channelId.value(), kind.name());
    }

    /** Postgres reads the lease as an interval, so the clock that decides expiry is the database's
     * - not the clock of whichever replica happens to be holding it. */
    private static String intervalOf(Duration duration) {
        return duration.toSeconds() + " seconds";
    }
}
