package dev.piovra.publication.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.piovra.common.Ids;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.publication.application.port.out.InventoryCache;
import dev.piovra.testsupport.PiovraIntegrationTest;

class InventoryCacheAdapterIT extends PiovraIntegrationTest {

    @Autowired
    private InventoryCache cache;

    @Test
    void upsert_then_available_for_round_trips() {
        Sku sku = Sku.of("TEST-" + Ids.newId());

        cache.upsert(TenantId.DEFAULT, sku, 8);

        assertThat(cache.availableFor(TenantId.DEFAULT, List.of(sku))).containsEntry(sku, 8);
    }

    @Test
    void a_sku_never_upserted_is_absent_from_the_result() {
        Sku sku = Sku.of("TEST-" + Ids.newId());

        Map<Sku, Integer> result = cache.availableFor(TenantId.DEFAULT, List.of(sku));

        assertThat(result).doesNotContainKey(sku);
    }

    @Test
    void a_second_upsert_replaces_the_first() {
        Sku sku = Sku.of("TEST-" + Ids.newId());

        cache.upsert(TenantId.DEFAULT, sku, 3);
        cache.upsert(TenantId.DEFAULT, sku, 9);

        assertThat(cache.availableFor(TenantId.DEFAULT, List.of(sku))).containsEntry(sku, 9);
    }
}
