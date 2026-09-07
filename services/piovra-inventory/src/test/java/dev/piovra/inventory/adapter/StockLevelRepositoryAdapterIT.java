package dev.piovra.inventory.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.piovra.common.Ids;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.inventory.adapter.out.persistence.StockLevelRepositoryAdapter;
import dev.piovra.inventory.domain.model.StockLevel;
import dev.piovra.testsupport.PiovraIntegrationTest;

class StockLevelRepositoryAdapterIT extends PiovraIntegrationTest {

    @Autowired
    private StockLevelRepositoryAdapter adapter;

    @Test
    void lock_or_create_returns_a_zeroed_level_for_an_unknown_sku() {
        Sku sku = Sku.of("TEST-" + Ids.newId());

        StockLevel level = adapter.lockOrCreate(TenantId.DEFAULT, sku);

        assertThat(level.onHand()).isZero();
        assertThat(level.available()).isZero();
    }

    @Test
    void ensure_exists_is_a_true_no_op_on_a_second_call() {
        Sku sku = Sku.of("TEST-" + Ids.newId());

        adapter.ensureExists(TenantId.DEFAULT, sku);
        StockLevel first = adapter.lockOrCreate(TenantId.DEFAULT, sku);
        adapter.save(new StockLevel(TenantId.DEFAULT, sku, 5, 0, 0, first.version() + 1));

        adapter.ensureExists(TenantId.DEFAULT, sku);
        StockLevel afterSecondEnsure = adapter.lockOrCreate(TenantId.DEFAULT, sku);

        assertThat(afterSecondEnsure.onHand()).isEqualTo(5);
    }

    @Test
    void save_persists_the_new_level() {
        Sku sku = Sku.of("TEST-" + Ids.newId());
        StockLevel before = adapter.lockOrCreate(TenantId.DEFAULT, sku);

        adapter.save(new StockLevel(TenantId.DEFAULT, sku, 42, 0, 0, before.version() + 1));

        StockLevel after = adapter.lockOrCreate(TenantId.DEFAULT, sku);
        assertThat(after.onHand()).isEqualTo(42);
    }
}
