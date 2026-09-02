package dev.piovra.driver.spi;

import java.util.List;

public record ListingPage(List<RemoteListing> listings, String nextCursor) {

    public ListingPage {
        listings = listings == null ? List.of() : List.copyOf(listings);
    }

    public boolean hasMore() {
        return nextCursor != null;
    }
}
