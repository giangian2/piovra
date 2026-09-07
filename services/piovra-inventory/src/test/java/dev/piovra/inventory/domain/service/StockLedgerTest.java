package dev.piovra.inventory.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.inventory.domain.model.StockLevel;

class StockLedgerTest {

    private static final TenantId TENANT = TenantId.of("acme");
    private static final Sku SKU = Sku.of("TSHIRT-BASE");

    @Test
    void apply_delta_adds_to_on_hand() {
        StockLevel current = new StockLevel(TENANT, SKU, 10, 0, 0, 1);

        StockLevel after = StockLedger.applyDelta(current, -3);

        assertThat(after.onHand()).isEqualTo(7);
        assertThat(after.available()).isEqualTo(7);
        assertThat(after.version()).isEqualTo(2);
    }

    @Test
    void apply_delta_can_take_on_hand_negative() {
        StockLevel current = new StockLevel(TENANT, SKU, 2, 0, 0, 1);

        StockLevel after = StockLedger.applyDelta(current, -5);

        assertThat(after.onHand()).isEqualTo(-3);
        assertThat(after.available()).isZero();
    }

    @Test
    void apply_set_replaces_on_hand_regardless_of_the_previous_value() {
        StockLevel current = new StockLevel(TENANT, SKU, 10, 0, 0, 1);

        StockLevel after = StockLedger.applySet(current, 42);

        assertThat(after.onHand()).isEqualTo(42);
        assertThat(after.version()).isEqualTo(2);
    }

    @Test
    void available_changed_is_true_when_available_moves() {
        StockLevel before = new StockLevel(TENANT, SKU, 10, 0, 0, 1);
        StockLevel after = StockLedger.applyDelta(before, -1);

        assertThat(StockLedger.availableChanged(before, after)).isTrue();
    }

    @Test
    void available_changed_is_false_when_on_hand_moves_but_available_stays_clamped_at_zero() {
        StockLevel before = new StockLevel(TENANT, SKU, 0, 0, 0, 1);
        StockLevel after = StockLedger.applyDelta(before, -5);

        assertThat(before.available()).isZero();
        assertThat(after.available()).isZero();
        assertThat(StockLedger.availableChanged(before, after)).isFalse();
    }
}
