package dev.piovra.order.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.piovra.common.Money;
import dev.piovra.common.Sku;
import dev.piovra.model.order.LineResolution;
import dev.piovra.model.order.OrderLine;

class LineResolverTest {

    private static final OrderLine RAW = new OrderLine(
            "line-1", "channel-line-1", "TSHIRT-BASE", null, LineResolution.UNMAPPED, 2, Money.euro("19.90"));

    @Test
    void a_resolved_sku_produces_a_mapped_line() {
        Sku sku = Sku.of("TSHIRT-BASE");

        OrderLine resolved = LineResolver.resolve(RAW, Optional.of(sku));

        assertThat(resolved.resolution()).isEqualTo(LineResolution.MAPPED);
        assertThat(resolved.sku()).isEqualTo(sku);
        assertThat(resolved.quantity()).isEqualTo(RAW.quantity());
    }

    @Test
    void an_unresolved_sku_produces_an_unmapped_line() {
        OrderLine resolved = LineResolver.resolve(RAW, Optional.empty());

        assertThat(resolved.resolution()).isEqualTo(LineResolution.UNMAPPED);
        assertThat(resolved.sku()).isNull();
    }

    @Test
    void resolution_never_mutates_the_line_identity_fields() {
        OrderLine resolved = LineResolver.resolve(RAW, Optional.of(Sku.of("TSHIRT-BASE")));

        assertThat(resolved.lineId()).isEqualTo(RAW.lineId());
        assertThat(resolved.channelLineId()).isEqualTo(RAW.channelLineId());
        assertThat(resolved.channelSku()).isEqualTo(RAW.channelSku());
        assertThat(resolved.unitPrice()).isEqualTo(RAW.unitPrice());
    }
}
