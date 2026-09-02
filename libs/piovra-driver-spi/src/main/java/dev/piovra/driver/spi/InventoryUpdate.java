package dev.piovra.driver.spi;

import dev.piovra.common.Sku;

/**
 * @param externalVariantId variant id on the marketplace, where one is needed
 * @param availableQuantity quantity already net of the channel buffer: the driver publishes this
 *     number without further adjustment
 */
public record InventoryUpdate(Sku sku, String externalId, String externalVariantId, int availableQuantity) {

    public InventoryUpdate {
        if (availableQuantity < 0) {
            throw new IllegalArgumentException("negative quantity for " + sku);
        }
    }
}
