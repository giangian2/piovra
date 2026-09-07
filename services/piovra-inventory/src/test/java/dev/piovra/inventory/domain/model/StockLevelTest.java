package dev.piovra.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;

class StockLevelTest {

    private static final TenantId TENANT = TenantId.of("acme");
    private static final Sku SKU = Sku.of("TSHIRT-BASE");

    @Test
    void available_is_never_negative_even_when_on_hand_is() {
        StockLevel level = new StockLevel(TENANT, SKU, -3, 0, 0, 1);

        assertThat(level.available()).isZero();
    }

    @Test
    void available_subtracts_reserved_and_buffer_from_on_hand() {
        StockLevel level = new StockLevel(TENANT, SKU, 10, 2, 1, 1);

        assertThat(level.available()).isEqualTo(7);
    }

    @Test
    void construction_always_recomputes_available_regardless_of_a_caller_supplied_value() {
        StockLevel level = new StockLevel(TENANT, SKU, 5, 0, 0, 999, 1);

        assertThat(level.available()).isEqualTo(5);
    }

    @Test
    void initial_is_all_zero() {
        StockLevel level = StockLevel.initial(TENANT, SKU);

        assertThat(level.onHand()).isZero();
        assertThat(level.reserved()).isZero();
        assertThat(level.buffer()).isZero();
        assertThat(level.available()).isZero();
        assertThat(level.version()).isZero();
    }
}
