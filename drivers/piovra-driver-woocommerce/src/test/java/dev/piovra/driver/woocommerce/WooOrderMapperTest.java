package dev.piovra.driver.woocommerce;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.piovra.common.ChannelId;
import dev.piovra.common.TenantId;
import dev.piovra.model.order.CanonicalOrder;
import dev.piovra.model.order.LineResolution;
import dev.piovra.model.order.OrderLine;
import dev.piovra.model.order.OrderStatus;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads a real WooCommerce page from a fixture: no HTTP, no Spring, no containers
 * (docs/12-development-guidelines.md section 6, "Domain" row).
 */
class WooOrderMapperTest {

    private static final TenantId TENANT = TenantId.of("acme");
    private static final ChannelId CHANNEL = ChannelId.of("woo-local");

    private final JsonNode page = load();

    @Test
    void a_processing_order_maps_to_paid() {
        assertThat(order(1001).status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void an_unknown_plugin_status_maps_to_new() {
        assertThat(order(1002).status()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    void an_unknown_plugin_status_is_kept_verbatim_in_channel_status() {
        assertThat(order(1002).channelStatus()).isEqualTo("awaiting-shipment");
    }

    @Test
    void gmt_dates_without_an_offset_are_read_as_utc() {
        assertThat(order(1001).placedAt()).isEqualTo(Instant.parse("2026-09-21T10:23:02Z"));
        assertThat(order(1001).lastModifiedAt()).isEqualTo(Instant.parse("2026-09-21T10:25:40Z"));
    }

    @Test
    void a_line_keeps_the_woocommerce_item_id_as_its_line_id() {
        assertThat(firstLine(1001).lineId()).isEqualTo("77");
    }

    @Test
    void a_line_arrives_unresolved_because_mapping_skus_belongs_to_the_order_service() {
        assertThat(firstLine(1001).resolution()).isEqualTo(LineResolution.UNMAPPED);
        assertThat(firstLine(1001).sku()).isNull();
        assertThat(firstLine(1001).channelSku()).isEqualTo("TSHIRT-BASE");
    }

    @Test
    void the_unit_price_is_the_line_total_divided_by_the_quantity() {
        assertThat(firstLine(1001).unitPrice().amount()).isEqualByComparingTo("19.90");
    }

    @Test
    void a_line_with_zero_quantity_is_skipped_instead_of_failing_the_page() {
        assertThat(order(1001).lines()).hasSize(1);
    }

    @Test
    void the_item_total_is_the_sum_of_the_lines_not_a_subtraction() {
        // total 70.00 - shipping 5.00 - tax 12.00 would be 53.00: a coupon sits in between.
        assertThat(order(1001).totals().items().amount()).isEqualByComparingTo("59.70");
        assertThat(order(1001).totals().grandTotal().amount()).isEqualByComparingTo("70.00");
    }

    @Test
    void the_shipping_address_is_used_when_woocommerce_fills_it() {
        assertThat(order(1001).shippingAddress().line1()).isEqualTo("Via Roma 1");
        assertThat(order(1001).shippingAddress().city()).isEqualTo("Milano");
    }

    @Test
    void the_shipping_address_falls_back_to_billing_when_woocommerce_leaves_it_empty() {
        assertThat(order(1003).shippingAddress().line1()).isEqualTo("Corso Buenos Aires 33");
    }

    @Test
    void a_guest_order_identifies_the_buyer_by_billing_email() {
        assertThat(order(1003).buyer().channelUserId()).isEqualTo("anna@test.it");
    }

    @Test
    void a_registered_buyer_is_identified_by_the_woocommerce_customer_id() {
        assertThat(order(1001).buyer().channelUserId()).isEqualTo("42");
    }

    @Test
    void a_line_without_a_sku_stays_traceable_through_its_variation_id() {
        assertThat(firstLine(1002).channelSku()).isEqualTo("woo:variation:9001");
    }

    @Test
    void a_checkout_draft_is_not_an_order() {
        assertThat(map(node(1004))).isEmpty();
    }

    private CanonicalOrder order(int id) {
        return map(node(id)).orElseThrow();
    }

    private OrderLine firstLine(int id) {
        return order(id).lines().getFirst();
    }

    private static Optional<CanonicalOrder> map(JsonNode node) {
        return WooOrderMapper.toCanonicalOrder(node, TENANT, CHANNEL);
    }

    private JsonNode node(int id) {
        for (JsonNode candidate : page) {
            if (candidate.path("id").asInt() == id) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("no order " + id + " in the fixture");
    }

    private static JsonNode load() {
        try (InputStream in = WooOrderMapperTest.class.getResourceAsStream("/woocommerce/orders-page.json")) {
            return JsonMapper.builder().build().readTree(in);
        } catch (Exception e) {
            throw new IllegalStateException("cannot read the fixture", e);
        }
    }
}
