package dev.piovra.driver.spi;

import dev.piovra.common.Money;
import dev.piovra.common.Sku;

/**
 * The state of a listing as the marketplace sees it. Used to detect drift: the diff trusts the
 * local snapshot, but if someone edits the listing by hand the only way to notice is to read it
 * back (docs/06-publish-flow.md section 8).
 */
public record RemoteListing(
        Sku sku, String externalId, String title, Money price, int availableQuantity, boolean active) {}
