package dev.piovra.driver.spi;

import java.time.Instant;

/**
 * Query window for orders.
 *
 * <p>Polling uses a sliding window with overlap ({@code from = lastSuccessfulPoll - 5 min}): the
 * overlap absorbs the marketplace's visibility lag and clock skew, and downstream deduplication
 * makes the duplicates harmless (docs/07-order-flow.md).
 *
 * @param cursor pagination token returned by the previous page, when the channel supports one
 */
public record OrderQuery(Instant modifiedFrom, Instant modifiedTo, int pageSize, String cursor) {

    public static OrderQuery since(Instant from, int pageSize) {
        return new OrderQuery(from, Instant.now(), pageSize, null);
    }

    public OrderQuery withCursor(String next) {
        return new OrderQuery(modifiedFrom, modifiedTo, pageSize, next);
    }
}
