package dev.piovra.driver.spi;

import java.util.Set;

/**
 * Descriptor of a channel's capabilities. The publication service queries it to decide which
 * command to emit and with what batch size: without it, every optimisation would turn into an
 * {@code if (channelType == EBAY)} scattered through the core.
 */
public record DriverCapabilities(
        boolean supportsVariations,
        boolean supportsBulkInventory,
        int maxBulkInventorySize,
        boolean supportsBulkUpsert,
        int maxBulkUpsertSize,
        /** When false, every change requires sending the listing's full payload. */
        boolean supportsPartialUpdate,
        boolean supportsWebhooks,
        /** When false, delisting is done by setting the quantity to zero. */
        boolean supportsEndListing,
        boolean requiresCategoryMapping,
        Set<String> supportedImageFormats,
        int maxImages,
        int maxTitleLength) {

    public int bulkInventorySize() {
        return supportsBulkInventory ? Math.max(1, maxBulkInventorySize) : 1;
    }

    public int bulkUpsertSize() {
        return supportsBulkUpsert ? Math.max(1, maxBulkUpsertSize) : 1;
    }
}
