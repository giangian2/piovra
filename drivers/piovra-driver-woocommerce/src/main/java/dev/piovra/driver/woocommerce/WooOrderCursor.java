package dev.piovra.driver.woocommerce;

import java.time.Instant;

/**
 * Opaque pagination token for the orders scan, serialized as {@code <epochSeconds>:<page>}.
 *
 * <p>WooCommerce is paged by ascending {@code modified} with a time cursor, never by page number
 * alone: pages shift under you when orders arrive during the scan (docs/08-marketplace-drivers.md).
 * But a pure time cursor has its own failure mode - if more than one page of orders shares a single
 * second, the cursor never advances and the scan loops forever. So the page number rides along and
 * is used only while the timestamp is standing still, which is what {@link #next} implements.
 */
record WooOrderCursor(Instant modifiedAfter, int page) {

    private static final int FIRST_PAGE = 1;

    static WooOrderCursor start(Instant modifiedFrom) {
        return new WooOrderCursor(modifiedFrom, FIRST_PAGE);
    }

    static WooOrderCursor parse(String raw, Instant fallback) {
        if (raw == null || raw.isBlank()) {
            return start(fallback);
        }
        int separator = raw.lastIndexOf(':');
        if (separator < 0) {
            return start(fallback);
        }
        return new WooOrderCursor(
                Instant.ofEpochSecond(Long.parseLong(raw.substring(0, separator))),
                Integer.parseInt(raw.substring(separator + 1)));
    }

    /**
     * @param lastModified the {@code date_modified_gmt} of the last order on the page just read
     * @param pageWasFull false when the page came back shorter than {@code per_page}, which is how
     *     WooCommerce says "that was the last one"
     * @return null when the scan is over
     */
    String next(Instant lastModified, boolean pageWasFull) {
        if (!pageWasFull || lastModified == null) {
            return null;
        }
        if (lastModified.isAfter(modifiedAfter)) {
            return new WooOrderCursor(lastModified, FIRST_PAGE).format();
        }
        // The whole page shares the cursor's own second: walk the pages until the clock moves.
        return new WooOrderCursor(modifiedAfter, page + 1).format();
    }

    String format() {
        return modifiedAfter.getEpochSecond() + ":" + page;
    }
}
