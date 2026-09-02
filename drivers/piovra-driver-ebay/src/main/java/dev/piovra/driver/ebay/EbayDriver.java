package dev.piovra.driver.ebay;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.piovra.common.ErrorClass;
import dev.piovra.driver.spi.ChannelContext;
import dev.piovra.driver.spi.DriverCapabilities;
import dev.piovra.driver.spi.DriverError;
import dev.piovra.driver.spi.EndListingRequest;
import dev.piovra.driver.spi.InventoryUpdate;
import dev.piovra.driver.spi.ListingPage;
import dev.piovra.driver.spi.ListingQuery;
import dev.piovra.driver.spi.ListingRequest;
import dev.piovra.driver.spi.MarketplaceDriver;
import dev.piovra.driver.spi.OrderPage;
import dev.piovra.driver.spi.OrderQuery;
import dev.piovra.driver.spi.PriceUpdate;
import dev.piovra.driver.spi.RemoteOrder;
import dev.piovra.driver.spi.UpdateResult;
import dev.piovra.driver.spi.UpsertResult;
import dev.piovra.model.channel.ChannelType;

/**
 * eBay driver (Sell REST APIs).
 *
 * <p><b>Status: skeleton, planned for phase 3.</b> WooCommerce comes first because its API is far
 * simpler and its sandbox is a local WordPress container: the end-to-end pipeline should be
 * validated before taking on eBay's aspects and taxonomy.
 *
 * <p>eBay's mental model: {@code InventoryItem} (the product, keyed by SKU) -&gt; {@code Offer} (the
 * listing on one marketplace, with price/quantity/policies) -&gt; {@code publish} (which yields the
 * listingId). SKU as the natural key is a gift: {@code createOrReplaceInventoryItem} is already an
 * upsert.
 *
 * <p>Planned APIs (docs/08-marketplace-drivers.md):
 *
 * <ul>
 *   <li>Inventory API: createOrReplaceInventoryItem, bulkCreateOrReplaceInventoryItem (max 25),
 *       createOffer/updateOffer/publishOffer, <b>bulkUpdatePriceQuantity</b> (max 25) - the
 *       endpoint on the anti-oversell path
 *   <li>Fulfillment API: getOrders filtered by lastmodifieddate, createShippingFulfillment
 *   <li>Account API: payment, shipping and return policies, referenced by id from the offer
 *   <li>Taxonomy API: getCategorySuggestions, getItemAspectsForCategory
 * </ul>
 *
 * <p>The clients are not written by hand: they are generated from the OpenAPI contracts eBay
 * publishes, using openapi-generator-maven-plugin at build time. The official Java SDK covers the
 * legacy Trading APIs and is dated.
 */
public class EbayDriver implements MarketplaceDriver {

    private static final DriverCapabilities CAPABILITIES = new DriverCapabilities(
            true, // supportsVariations: inventory item group
            true, // supportsBulkInventory
            25, // maxBulkInventorySize: the bulkUpdatePriceQuantity limit
            true, // supportsBulkUpsert
            25, // maxBulkUpsertSize: the bulkCreateOrReplaceInventoryItem limit
            true, // supportsPartialUpdate
            false, // supportsWebhooks: eBay uses notifications, not classic webhooks
            true, // supportsEndListing: withdrawOffer, though zeroing the quantity is preferred
            true, // requiresCategoryMapping: category and aspects are mandatory
            Set.of("jpg", "jpeg", "png"),
            24,
            80); // maxTitleLength: 80 characters, eBay's long-standing limit

    @Override
    public ChannelType type() {
        return ChannelType.EBAY;
    }

    @Override
    public DriverCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public UpsertResult upsertListing(ChannelContext ctx, ListingRequest request) {
        // TODO phase 3:
        //  1. createOrReplaceInventoryItem (already idempotent on the SKU)
        //  2. for variants: createOrReplaceInventoryItemGroup
        //  3. createOffer or updateOffer with category, aspects and policy ids
        //  4. publishOffer -> listingId. Publishing is asynchronous on eBay's side: the real state
        //     is verified during reconciliation.
        throw new UnsupportedOperationException("EbayDriver.upsertListing: planned for phase 3");
    }

    @Override
    public UpdateResult updateInventory(ChannelContext ctx, List<InventoryUpdate> updates) {
        // TODO phase 3: bulkUpdatePriceQuantity in chunks of 25. This is the critical
        //  anti-oversell path: small batches, high priority, reserved quota.
        throw new UnsupportedOperationException("EbayDriver.updateInventory: planned for phase 3");
    }

    @Override
    public UpdateResult updatePrice(ChannelContext ctx, List<PriceUpdate> updates) {
        // TODO phase 3: the same bulkUpdatePriceQuantity endpoint.
        throw new UnsupportedOperationException("EbayDriver.updatePrice: planned for phase 3");
    }

    @Override
    public UpdateResult endListing(ChannelContext ctx, EndListingRequest request) {
        // TODO phase 3: prefer zeroing the quantity (it preserves the listing's history and
        //  ranking); withdrawOffer only when the policy explicitly asks for it.
        throw new UnsupportedOperationException("EbayDriver.endListing: planned for phase 3");
    }

    @Override
    public OrderPage fetchOrders(ChannelContext ctx, OrderQuery query) {
        // TODO phase 3: getOrders filtered by lastmodifieddate, limit/offset pagination.
        throw new UnsupportedOperationException("EbayDriver.fetchOrders: planned for phase 3");
    }

    @Override
    public Optional<RemoteOrder> fetchOrder(ChannelContext ctx, String channelOrderId) {
        throw new UnsupportedOperationException("EbayDriver.fetchOrder: planned for phase 3");
    }

    @Override
    public ListingPage fetchListings(ChannelContext ctx, ListingQuery query) {
        throw new UnsupportedOperationException("EbayDriver.fetchListings: planned for phase 3");
    }

    @Override
    public DriverError translate(Exception e) {
        // TODO phase 3: map eBay's real codes. Some already-known cases:
        //  - 25002 duplicate listing        -> MARKETPLACE_REJECT, not retryable
        //  - 25007 missing required aspect  -> MAPPING, with the missing aspects listed in
        //    suggestedAction: it is the most frequent error and must say what to add to the feed
        //  - invalid_grant on refresh       -> AUTH, immediate alert: a human must re-consent
        //  - 429 / daily quota              -> RATE_LIMIT, with Retry-After when present
        return DriverError.of(ErrorClass.INTERNAL, "EBAY_NOT_IMPLEMENTED", e.getMessage());
    }
}
