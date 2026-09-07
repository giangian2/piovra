package dev.piovra.inventory.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.inventory.application.port.out.StockLevelRepository;
import dev.piovra.inventory.domain.model.StockLevel;

@Repository
public class StockLevelRepositoryAdapter implements StockLevelRepository {

    private final StockLevelJpaRepository jpaRepository;
    private final StockLevelEntityMapper mapper;
    private final JdbcTemplate jdbcTemplate;

    public StockLevelRepositoryAdapter(
            StockLevelJpaRepository jpaRepository, StockLevelEntityMapper mapper, JdbcTemplate jdbcTemplate) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Insert-then-lock: the plain JDBC insert races safely on the {@code (tenant_id, sku)} unique
     * constraint, so no matter which concurrent caller wins the insert, every caller's subsequent
     * {@code findForUpdate} sees exactly one row and blocks on its lock as expected. */
    @Override
    public StockLevel lockOrCreate(TenantId tenantId, Sku sku) {
        insertIfAbsent(tenantId, sku);
        return jpaRepository
                .findForUpdate(tenantId.value(), sku.value())
                .map(mapper::toDomain)
                .orElseThrow(() -> new IllegalStateException(
                        "stock_level row missing right after insert: " + tenantId + "/" + sku));
    }

    @Override
    public void save(StockLevel level) {
        StockLevelEntity entity = jpaRepository
                .findByTenantIdAndSku(level.tenantId().value(), level.sku().value())
                .map(existing -> {
                    mapper.applyTo(existing, level);
                    return existing;
                })
                .orElseGet(() -> mapper.toNewEntity(level));
        jpaRepository.save(entity);
    }

    @Override
    public void ensureExists(TenantId tenantId, Sku sku) {
        insertIfAbsent(tenantId, sku);
    }

    private void insertIfAbsent(TenantId tenantId, Sku sku) {
        jdbcTemplate.update(
                "INSERT INTO inventory.stock_level "
                        + "(id, tenant_id, sku, on_hand, reserved, buffer, available, version, updated_at) "
                        + "VALUES (?, ?, ?, 0, 0, 0, 0, 0, ?) ON CONFLICT (tenant_id, sku) DO NOTHING",
                UUID.randomUUID(),
                tenantId.value(),
                sku.value(),
                Timestamp.from(Instant.now()));
    }
}
