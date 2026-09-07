package dev.piovra.inventory.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.events.InventoryChanged;
import dev.piovra.inventory.application.port.out.StockMovementRepository;

/**
 * Plain JDBC, no JPA entity: the table has no behaviour beyond an idempotent append, same reasoning
 * as {@code PublicationIdempotencyStore}. The unique constraint is {@code (tenant_id,
 * idempotency_key)}, not {@code idempotency_key} alone - the docs/02-services.md schema sketch omits
 * {@code tenant_id} for brevity, but every other table in this codebase is tenant-scoped and an
 * idempotency key here (an order/line pair, or a feed batch id) is not guaranteed globally unique
 * across tenants.
 */
@Repository
public class StockMovementRepositoryAdapter implements StockMovementRepository {

    private final JdbcTemplate jdbcTemplate;

    public StockMovementRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Long> append(
            TenantId tenantId,
            Sku sku,
            int delta,
            InventoryChanged.Reason reason,
            ChannelId sourceChannelId,
            String idempotencyKey) {
        List<Long> ids = jdbcTemplate.query(
                "INSERT INTO inventory.stock_movement "
                        + "(tenant_id, sku, delta, reason, source_channel, idempotency_key, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (tenant_id, idempotency_key) DO NOTHING RETURNING id",
                (rs, rowNum) -> rs.getLong("id"),
                tenantId.value(),
                sku.value(),
                delta,
                reason.name(),
                sourceChannelId == null ? null : sourceChannelId.value(),
                idempotencyKey,
                Timestamp.from(Instant.now()));
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.getFirst());
    }
}
