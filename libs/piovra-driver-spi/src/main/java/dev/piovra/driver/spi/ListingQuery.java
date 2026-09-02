package dev.piovra.driver.spi;

import java.time.Instant;

/** Query used by reconciliation to read the real state on the marketplace. */
public record ListingQuery(Instant modifiedFrom, int pageSize, String cursor) {

    public ListingQuery withCursor(String next) {
        return new ListingQuery(modifiedFrom, pageSize, next);
    }
}
