package dev.piovra.driver.woocommerce;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
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
 * WooCommerce driver (REST API v3).
 *
 * <p><b>Status: skeleton.</b> The structure and the pitfalls are settled, the method bodies are not.
 *
 * <p>Implementation notes, from docs/08-marketplace-drivers.md:
 *
 * <ul>
 *   <li><b>Upsert</b>: Woo has no writable natural key. With an external_id we PUT directly;
 *       without one we GET /products?sku= (lookup-before-create) and adopt the id if it exists.
 *       Creating blindly after a timeout produces duplicates.
 *   <li><b>Deceptive timeouts</b>: a POST /products/batch that timed out may still have been
 *       executed. The retry must always go through the lookup.
 *   <li><b>Performance</b>: WordPress is the bottleneck. Always /batch (max 100), per_page=100, and
 *       _fields to shrink the response payload.
 *   <li><b>Orders</b>: page by ascending modified with a time cursor, never by page number: pages
 *       shift when orders arrive during the scan.
 *   <li><b>Time zones</b>: dates_are_gmt=true, everything normalized to UTC.
 * </ul>
 */
public class WooCommerceDriver implements MarketplaceDriver {

    private static final DriverCapabilities CAPABILITIES = new DriverCapabilities(
            true, // supportsVariations
            true, // supportsBulkInventory
            100, // maxBulkInventorySize: the /products/batch limit
            true, // supportsBulkUpsert
            100, // maxBulkUpsertSize
            true, // supportsPartialUpdate: PUT accepts only the fields to change
            true, // supportsWebhooks
            false, // supportsEndListing: we move to outofstock/private, we do not "end" a listing
            false, // requiresCategoryMapping: Woo accepts free-form categories
            Set.of("jpg", "jpeg", "png", "webp"),
            20,
            255);

    private final WooCommerceApiClient client;

    public WooCommerceDriver(WooCommerceApiClient client) {
        this.client = client;
    }

    @Override
    public ChannelType type() {
        return ChannelType.WOOCOMMERCE;
    }

    @Override
    public DriverCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public UpsertResult upsertListing(ChannelContext ctx, ListingRequest request) {
        // TODO phase 1:
        //  1. known externalId -> PUT /products/{id}
        //  2. otherwise GET /products?sku= -> adopt the id if found, else POST /products
        //  3. variable products: parent with type=variable, then /products/{id}/variations/batch
        //  4. store the returned id BEFORE treating the command as successful
        throw new UnsupportedOperationException("WooCommerceDriver.upsertListing: not implemented yet");
    }

    @Override
    public UpdateResult updateInventory(ChannelContext ctx, List<InventoryUpdate> updates) {
        // TODO phase 1: POST /products/batch with only stock_quantity/stock_status, in chunks of
        //  capabilities().bulkInventorySize(). Failures must be reported per item: in a batch of
        //  100, three of them can fail.
        throw new UnsupportedOperationException("WooCommerceDriver.updateInventory: not implemented yet");
    }

    @Override
    public UpdateResult updatePrice(ChannelContext ctx, List<PriceUpdate> updates) {
        // TODO phase 1: POST /products/batch with regular_price / sale_price.
        throw new UnsupportedOperationException("WooCommerceDriver.updatePrice: not implemented yet");
    }

    @Override
    public UpdateResult endListing(ChannelContext ctx, EndListingRequest request) {
        // TODO phase 1: stock_status=outofstock, or status=private when the policy asks for it.
        throw new UnsupportedOperationException("WooCommerceDriver.endListing: not implemented yet");
    }

    @Override
    public OrderPage fetchOrders(ChannelContext ctx, OrderQuery query) {
        // TODO phase 2: GET /orders?modified_after=&dates_are_gmt=true&orderby=modified&order=asc
        //  &per_page=100. The cursor is the timestamp of the last order on the page.
        throw new UnsupportedOperationException("WooCommerceDriver.fetchOrders: not implemented yet");
    }

    @Override
    public Optional<RemoteOrder> fetchOrder(ChannelContext ctx, String channelOrderId) {
        // TODO phase 2: GET /orders/{id}. Used by the webhook, which is only an accelerator: the
        //  webhook payload is never treated as authoritative.
        throw new UnsupportedOperationException("WooCommerceDriver.fetchOrder: not implemented yet");
    }

    @Override
    public ListingPage fetchListings(ChannelContext ctx, ListingQuery query) {
        // TODO phase 2: GET /products?modified_after=&_fields=id,sku,price,stock_quantity,status
        throw new UnsupportedOperationException("WooCommerceDriver.fetchListings: not implemented yet");
    }

    /**
     * Error translation. This is the method that decides whether the system will retry: write it by
     * reading Woo's real error codes, not by guessing from the HTTP status.
     */
    @Override
    public DriverError translate(Exception e) {
        return switch (e) {
            case WooApiException api -> translateApi(api);
            case HttpTimeoutException ignored ->
                DriverError.of(ErrorClass.TRANSIENT, "WOO_TRANSIENT_TIMEOUT", "timeout verso il negozio WooCommerce");
            case SocketTimeoutException ignored ->
                DriverError.of(ErrorClass.TRANSIENT, "WOO_TRANSIENT_TIMEOUT", "read timeout");
            default -> DriverError.internal("unhandled error in the WooCommerce driver", e);
        };
    }

    private DriverError translateApi(WooApiException e) {
        int status = e.status();
        // 401/403: consumer key revoked or insufficient permissions. Retrying is pointless.
        if (status == 401 || status == 403) {
            return new DriverError(
                    ErrorClass.AUTH,
                    "WOO_AUTH_" + status,
                    e.wooCode(),
                    "WooCommerce credentials rejected",
                    "Regenerate the consumer key/secret pair in the store and update it in Vault.",
                    status,
                    null);
        }
        if (status == 429) {
            return new DriverError(
                    ErrorClass.RATE_LIMIT,
                    "WOO_RATE_LIMIT",
                    e.wooCode(),
                    "troppe richieste verso il negozio",
                    null,
                    status,
                    e.retryAfter());
        }
        if (status >= 500) {
            return new DriverError(
                    ErrorClass.TRANSIENT,
                    "WOO_TRANSIENT_" + status,
                    e.wooCode(),
                    "il negozio ha risposto " + status,
                    null,
                    status,
                    null);
        }
        if (status == 400 || status == 404) {
            return new DriverError(
                    ErrorClass.VALIDATION,
                    "WOO_VALIDATION_" + e.wooCode(),
                    e.wooCode(),
                    e.getMessage(),
                    "Check the product data in the feed: WooCommerce rejected the payload.",
                    status,
                    null);
        }
        return new DriverError(
                ErrorClass.MARKETPLACE_REJECT,
                "WOO_REJECT_" + e.wooCode(),
                e.wooCode(),
                e.getMessage(),
                "Apri il prodotto nel backend WooCommerce e verifica il motivo del rifiuto.",
                status,
                null);
    }
}
