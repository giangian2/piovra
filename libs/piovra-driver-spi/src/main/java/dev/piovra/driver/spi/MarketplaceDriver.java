package dev.piovra.driver.spi;

import java.util.List;
import java.util.Optional;

import dev.piovra.model.channel.ChannelType;

/**
 * Contract of a marketplace driver.
 *
 * <p><b>A driver translates, it does not decide.</b> It never consults the catalog, does not know
 * whether a product should be published, and knows nothing about Kafka or Spring. It receives an
 * already-decided request, converts it into the marketplace's dialect, executes it and reports the
 * outcome.
 *
 * <p>Implementations must be <b>stateless and thread-safe</b>: a single bean per channel type
 * serves every account, and concurrency is that of virtual threads. Everything account-specific
 * travels in {@link ChannelContext}.
 *
 * <p>Every implementation must pass the TCK in {@code piovra-driver-tck}.
 */
public interface MarketplaceDriver {

    ChannelType type();

    /**
     * What this channel can actually do. Declaring it optimistically is the fastest way to break
     * the system: the publication service uses these flags to choose commands.
     */
    DriverCapabilities capabilities();

    // ------------------------------------------------------------------ outbound: catalog

    /** Creates the listing if it does not exist, otherwise updates it. Idempotent. */
    UpsertResult upsertListing(ChannelContext ctx, ListingRequest request);

    /**
     * Updates quantities only. This is the critical anti-oversell path: it must use the lightest
     * endpoint available and, where one exists, the bulk variant.
     */
    UpdateResult updateInventory(ChannelContext ctx, List<InventoryUpdate> updates);

    /** Updates prices only. */
    UpdateResult updatePrice(ChannelContext ctx, List<PriceUpdate> updates);

    /**
     * Removes the product from the channel. What "remove" means is the driver's call: on eBay it is
     * better to zero the quantity so history and ranking survive, on WooCommerce the product moves
     * to outofstock or private.
     */
    UpdateResult endListing(ChannelContext ctx, EndListingRequest request);

    // ------------------------------------------------------------------ inbound: orders

    /** One page of orders. The implementation must guarantee a stable ordering and a repeatable
     * cursor. */
    OrderPage fetchOrders(ChannelContext ctx, OrderQuery query);

    Optional<RemoteOrder> fetchOrder(ChannelContext ctx, String channelOrderId);

    // ------------------------------------------------------------------ reconciliation

    /** The real listing state on the marketplace, used to detect drift (docs/06 section 8). */
    ListingPage fetchListings(ChannelContext ctx, ListingQuery query);

    /**
     * Translates a native exception into the canonical taxonomy. This is the method that decides
     * whether the system will retry: write it by reading the marketplace's real error codes, not by
     * guessing from the HTTP status.
     */
    DriverError translate(Exception exception);
}
