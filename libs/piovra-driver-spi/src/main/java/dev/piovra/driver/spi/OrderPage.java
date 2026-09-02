package dev.piovra.driver.spi;

import java.util.List;
import java.util.Optional;

/**
 * @param nextCursor null when there are no further pages. The connector advances the persisted
 *     cursor only after publishing to Kafka: losing a page means losing orders.
 */
public record OrderPage(List<RemoteOrder> orders, String nextCursor, int totalHint) {

    public OrderPage {
        orders = orders == null ? List.of() : List.copyOf(orders);
    }

    public static OrderPage last(List<RemoteOrder> orders) {
        return new OrderPage(orders, null, orders.size());
    }

    public boolean hasMore() {
        return nextCursor != null;
    }

    public Optional<String> next() {
        return Optional.ofNullable(nextCursor);
    }
}
