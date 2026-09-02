package dev.piovra.driver.spi;

import dev.piovra.common.Sku;

/**
 * @param reason why the product is being delisted, recorded in the audit log and useful in the
 *     console
 * @param preferZeroQuantity when true the driver zeroes the quantity instead of ending the listing,
 *     wherever that distinction exists
 */
public record EndListingRequest(
        Sku sku, String externalId, String reason, boolean preferZeroQuantity, String commandId) {}
