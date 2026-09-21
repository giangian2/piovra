package dev.piovra.connector.woocommerce.domain;

import java.time.Duration;
import java.time.Instant;

import dev.piovra.driver.spi.OrderQuery;

/**
 * The sliding window a poll asks the marketplace about.
 *
 * <p>It reaches back one overlap before the cursor on purpose: marketplaces make an order visible
 * some seconds after it exists, and clocks disagree. Re-reading that overlap is what stops orders
 * from falling into the gap; the duplicates it produces cost nothing, because
 * {@code (channelId, channelOrderId)} is UNIQUE in the order service (docs/07-order-flow.md
 * section 2.1).
 *
 * <p>Zero framework: a record and a subtraction, testable without booting anything.
 */
public record PollWindow(Instant from, Instant to) {

    public static PollWindow of(Instant cursorAt, Duration overlap, Instant now) {
        Instant from = cursorAt.minus(overlap);
        // A cursor ahead of the clock would otherwise produce an inverted window.
        return new PollWindow(from.isAfter(now) ? now : from, now);
    }

    public OrderQuery toOrderQuery(int pageSize, String pageCursor) {
        return new OrderQuery(from, to, pageSize, pageCursor);
    }
}
