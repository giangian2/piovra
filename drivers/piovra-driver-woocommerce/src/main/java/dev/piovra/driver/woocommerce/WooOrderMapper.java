package dev.piovra.driver.woocommerce;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Money;
import dev.piovra.common.TenantId;
import dev.piovra.model.order.Address;
import dev.piovra.model.order.Buyer;
import dev.piovra.model.order.CanonicalOrder;
import dev.piovra.model.order.LineResolution;
import dev.piovra.model.order.OrderLine;
import dev.piovra.model.order.OrderStatus;
import dev.piovra.model.order.OrderTotals;

import tools.jackson.databind.JsonNode;

/**
 * Translates one WooCommerce order into the canonical model. A pure function: no I/O, no state, no
 * framework - which is what lets it be tested in milliseconds against a JSON fixture.
 *
 * <p>SKUs are deliberately left unresolved ({@link LineResolution#UNMAPPED}, {@code sku = null}):
 * mapping a channel SKU to a canonical one needs the catalog, which the order service owns. The
 * driver translates, it does not decide.
 *
 * <p>No WooCommerce status corresponds to {@link OrderStatus#SHIPPED}: Woo goes straight from
 * {@code processing} to {@code completed}. That is a property of the marketplace, not an omission.
 */
final class WooOrderMapper {

    private WooOrderMapper() {}

    /**
     * @return empty when the node is not an order at all - {@code checkout-draft} is an abandoned
     *     cart that Woo exposes on the same endpoint.
     */
    static Optional<CanonicalOrder> toCanonicalOrder(JsonNode order, TenantId tenantId, ChannelId channelId) {
        String nativeStatus = text(order, "status");
        if ("checkout-draft".equals(nativeStatus)) {
            return Optional.empty();
        }

        Currency currency = currencyOf(order);
        JsonNode billing = order.path("billing");
        JsonNode shipping = order.path("shipping");

        return Optional.of(new CanonicalOrder(
                null, // the order service mints the canonical id on insert
                tenantId,
                channelId,
                order.path("id").asString(),
                statusOf(nativeStatus),
                nativeStatus,
                instant(order, "date_created_gmt"),
                instant(order, "date_modified_gmt"),
                buyer(order, billing),
                address(hasStreet(shipping) ? shipping : billing),
                totals(order, currency),
                lines(order, currency),
                false));
    }

    /**
     * Unknown statuses - WooCommerce plugins add their own freely - map to {@code NEW} rather than
     * throwing: one unexpected value must not stop a whole page of orders. Nothing is lost, because
     * the native string is kept verbatim in {@code channelStatus}.
     */
    private static OrderStatus statusOf(String nativeStatus) {
        return switch (nativeStatus == null ? "" : nativeStatus) {
            case "processing" -> OrderStatus.PAID;
            case "completed" -> OrderStatus.COMPLETED;
            case "cancelled", "failed", "trash" -> OrderStatus.CANCELLED;
            case "refunded" -> OrderStatus.REFUNDED;
            default -> OrderStatus.NEW;
        };
    }

    private static Buyer buyer(JsonNode order, JsonNode billing) {
        String email = text(billing, "email");
        int customerId = order.path("customer_id").asInt(0);
        // A guest checkout has customer_id 0: the email is the only stable identifier left.
        String channelUserId = customerId != 0 ? String.valueOf(customerId) : email;
        return new Buyer(channelUserId, fullName(billing), email);
    }

    private static Address address(JsonNode block) {
        return new Address(
                fullName(block),
                text(block, "address_1"),
                text(block, "address_2"),
                text(block, "city"),
                text(block, "state"),
                text(block, "postcode"),
                text(block, "country"),
                text(block, "phone"));
    }

    /** WooCommerce leaves the shipping block empty on virtual orders; billing is then the only address. */
    private static boolean hasStreet(JsonNode block) {
        return text(block, "address_1") != null;
    }

    private static OrderTotals totals(JsonNode order, Currency currency) {
        BigDecimal items = BigDecimal.ZERO;
        for (JsonNode line : order.path("line_items")) {
            items = items.add(decimal(line, "total"));
        }
        // items is summed from the lines rather than derived as total - shipping - tax: subtracting
        // leaves rounding artifacts whenever a coupon or a fee is in play.
        return new OrderTotals(
                money(items, currency),
                money(decimal(order, "shipping_total"), currency),
                money(decimal(order, "total_tax"), currency),
                money(decimal(order, "total"), currency));
    }

    private static List<OrderLine> lines(JsonNode order, Currency currency) {
        List<OrderLine> lines = new ArrayList<>();
        for (JsonNode item : order.path("line_items")) {
            int quantity = item.path("quantity").asInt(0);
            if (quantity <= 0) {
                // A refunded or zeroed line: OrderLine rejects it by contract, and one odd line
                // must not fail the whole page.
                continue;
            }
            // The WooCommerce line id, never a fresh one: it goes into the stock movement's
            // idempotency key, so a new value on a re-read would decrement the stock twice.
            String lineId = item.path("id").asString();
            lines.add(new OrderLine(
                    lineId,
                    lineId,
                    channelSku(item),
                    null,
                    LineResolution.UNMAPPED,
                    quantity,
                    unitPrice(item, quantity, currency)));
        }
        return lines;
    }

    /**
     * A WooCommerce product may legitimately have no SKU. Falling back to its id keeps the line
     * traceable - it stays UNMAPPED either way, but an operator can still find what was sold.
     */
    private static String channelSku(JsonNode item) {
        String sku = text(item, "sku");
        if (sku != null) {
            return sku;
        }
        int variationId = item.path("variation_id").asInt(0);
        return variationId != 0
                ? "woo:variation:" + variationId
                : "woo:product:" + item.path("product_id").asString();
    }

    /**
     * What the buyer actually paid per unit: {@code total} already has the line's discounts applied,
     * whereas {@code price} is a JSON float carrying the list price.
     */
    private static Money unitPrice(JsonNode item, int quantity, Currency currency) {
        BigDecimal unit = decimal(item, "total")
                .divide(BigDecimal.valueOf(quantity), currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        return new Money(unit, currency);
    }

    private static Currency currencyOf(JsonNode order) {
        String code = text(order, "currency");
        return Currency.getInstance(code == null ? "EUR" : code);
    }

    private static Money money(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    /** WooCommerce sends monetary values as strings, so {@code decimalValue()} would return zero. */
    private static BigDecimal decimal(JsonNode node, String field) {
        String raw = text(node, field);
        return raw == null ? BigDecimal.ZERO : new BigDecimal(raw);
    }

    /** The {@code _gmt} fields carry no offset at all: they are UTC by name, not by notation. */
    private static Instant instant(JsonNode node, String field) {
        String raw = text(node, field);
        return raw == null ? null : LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC);
    }

    private static String fullName(JsonNode block) {
        String name = (orEmpty(text(block, "first_name")) + " " + orEmpty(text(block, "last_name"))).trim();
        return name.isEmpty() ? null : name;
    }

    /** Blank and absent mean the same thing in a WooCommerce payload, so both become null. */
    private static String text(JsonNode node, String field) {
        String value = node.path(field).asString("").trim();
        return value.isEmpty() ? null : value;
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
