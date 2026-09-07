package dev.piovra.order.adapter.out.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.order.application.port.out.KnownSkuRepository;

/** Plain JDBC, no JPA entity: the table has no behaviour beyond "is this SKU known", same
 * minimalism as {@code StockMovementRepositoryAdapter} in piovra-inventory. */
@Repository
public class KnownSkuRepositoryAdapter implements KnownSkuRepository {

    private final JdbcTemplate jdbcTemplate;

    public KnownSkuRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean exists(TenantId tenantId, Sku sku) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM orders.known_sku WHERE tenant_id = ? AND sku = ?",
                Integer.class,
                tenantId.value(),
                sku.value());
        return count != null && count > 0;
    }

    @Override
    public void ensureExists(TenantId tenantId, Sku sku) {
        jdbcTemplate.update(
                "INSERT INTO orders.known_sku (tenant_id, sku) VALUES (?, ?) ON CONFLICT (tenant_id, sku) DO NOTHING",
                tenantId.value(),
                sku.value());
    }
}
