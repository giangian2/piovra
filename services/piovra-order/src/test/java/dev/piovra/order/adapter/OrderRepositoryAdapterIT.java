package dev.piovra.order.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.Money;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.model.order.Address;
import dev.piovra.model.order.Buyer;
import dev.piovra.model.order.CanonicalOrder;
import dev.piovra.model.order.LineResolution;
import dev.piovra.model.order.OrderLine;
import dev.piovra.model.order.OrderStatus;
import dev.piovra.model.order.OrderTotals;
import dev.piovra.order.adapter.out.persistence.OrderRepositoryAdapter;
import dev.piovra.testsupport.PiovraIntegrationTest;

class OrderRepositoryAdapterIT extends PiovraIntegrationTest {

    private static final TenantId TENANT = TenantId.DEFAULT;
    private static final ChannelId CHANNEL = ChannelId.of("test-channel");

    @Autowired
    private OrderRepositoryAdapter adapter;

    @Test
    void round_trips_an_order() {
        CanonicalOrder order = order("CH-" + Ids.newId());

        adapter.save(order);
        Optional<CanonicalOrder> found = adapter.findByChannelOrderId(TENANT, CHANNEL, order.channelOrderId());

        assertThat(found).contains(order);
    }

    @Test
    void find_by_order_id_round_trips_too() {
        CanonicalOrder order = order("CH-" + Ids.newId());
        adapter.save(order);

        assertThat(adapter.findByOrderId(TENANT, order.orderId())).contains(order);
    }

    @Test
    void a_second_save_updates_the_same_row_instead_of_creating_a_new_one() {
        CanonicalOrder order = order("CH-" + Ids.newId());
        adapter.save(order);

        CanonicalOrder updated = new CanonicalOrder(
                order.orderId(),
                order.tenantId(),
                order.channelId(),
                order.channelOrderId(),
                OrderStatus.CANCELLED,
                order.channelStatus(),
                order.placedAt(),
                Instant.now(),
                order.buyer(),
                order.shippingAddress(),
                order.totals(),
                order.lines(),
                order.stockApplied());
        adapter.save(updated);

        Optional<CanonicalOrder> found = adapter.findByChannelOrderId(TENANT, CHANNEL, order.channelOrderId());
        assertThat(found).map(CanonicalOrder::status).contains(OrderStatus.CANCELLED);
    }

    private static CanonicalOrder order(String channelOrderId) {
        Sku sku = Sku.of("TEST-" + Ids.newId());
        OrderLine line = new OrderLine(
                "line-1", "channel-line-1", sku.value(), sku, LineResolution.MAPPED, 1, Money.euro("19.90"));
        return new CanonicalOrder(
                Ids.newId(),
                TENANT,
                CHANNEL,
                channelOrderId,
                OrderStatus.NEW,
                "processing",
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                new Buyer("buyer-1", "Mario Rossi", "mario@test.it"),
                new Address("Mario Rossi", "Via Roma 1", null, "Milano", "MI", "20100", "IT", null),
                new OrderTotals(Money.euro("19.90"), Money.euro("0.00"), Money.euro("0.00"), Money.euro("19.90")),
                List.of(line),
                false);
    }
}
