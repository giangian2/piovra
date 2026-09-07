package dev.piovra.app.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.Money;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.events.OrderReceived;
import dev.piovra.events.Topics;
import dev.piovra.inventory.adapter.out.persistence.StockLevelRepositoryAdapter;
import dev.piovra.inventory.domain.model.StockLevel;
import dev.piovra.model.order.Address;
import dev.piovra.model.order.Buyer;
import dev.piovra.model.order.CanonicalOrder;
import dev.piovra.model.order.LineResolution;
import dev.piovra.model.order.OrderLine;
import dev.piovra.model.order.OrderStatus;
import dev.piovra.model.order.OrderTotals;
import dev.piovra.order.application.port.out.KnownSkuRepository;
import dev.piovra.testsupport.PiovraIntegrationTest;

/**
 * Proves the order -&gt; stock loop closes end to end, not just module by module: a {@code
 * channel.order.received} message ingested by piovra-order results in a real decrement in
 * piovra-inventory's stock_level, via {@code OrderAccepted} - exactly the chain
 * docs/07-order-flow.md section 1 describes, now that both ends exist.
 */
class OrderToInventoryLoopIT extends PiovraIntegrationTest {

    private static final TenantId TENANT = TenantId.DEFAULT;
    private static final ChannelId CHANNEL = ChannelId.of("test-channel");

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KnownSkuRepository knownSkuRepository;

    @Autowired
    private StockLevelRepositoryAdapter stockLevelRepository;

    @Test
    void an_order_decrements_stock_across_the_whole_chain() throws Exception {
        Sku sku = Sku.of("TEST-" + Ids.newId());
        knownSkuRepository.ensureExists(TENANT, sku);
        stockLevelRepository.save(new StockLevel(
                TENANT,
                sku,
                10,
                0,
                0,
                stockLevelRepository.lockOrCreate(TENANT, sku).version() + 1));

        OrderLine line = new OrderLine(
                "line-1", "channel-line-1", sku.value(), null, LineResolution.UNMAPPED, 3, Money.euro("19.90"));
        CanonicalOrder incoming = new CanonicalOrder(
                Ids.newId(),
                TENANT,
                CHANNEL,
                "CH-" + Ids.newId(),
                OrderStatus.NEW,
                "processing",
                Instant.now(),
                Instant.now(),
                new Buyer("buyer-1", "Mario Rossi", "mario@test.it"),
                new Address("Mario Rossi", "Via Roma 1", null, "Milano", "MI", "20100", "IT", null),
                new OrderTotals(Money.euro("19.90"), Money.euro("0.00"), Money.euro("0.00"), Money.euro("19.90")),
                List.of(line),
                false);
        OrderReceived event = OrderReceived.of(incoming, null);

        kafkaTemplate
                .send(Topics.CHANNEL_ORDER_RECEIVED, event.partitionKey(), objectMapper.writeValueAsString(event))
                .get();

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(
                        stockLevelRepository.lockOrCreate(TENANT, sku).onHand())
                .isEqualTo(7));
    }
}
