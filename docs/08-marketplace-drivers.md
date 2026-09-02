# 08 — Marketplace drivers

## 1. Philosophy

A driver is a **translator, not a decision maker**. It receives canonical commands, converts them
into the marketplace's dialect, executes, and reports the outcome. It never consults the catalog,
never decides whether to publish, and contains no business rules. The practical consequence: adding
Amazon or Shopify means writing a new module, without touching the core.

Every driver is **both**:
- a **library** (`piovra-driver-<x>`) implementing the SPI, testable in isolation;
- a **service** (`connector-<x>`) hosting the library, the Kafka consumers, the polling scheduler and
  the rate limiter.

## 2. The SPI

Module `piovra-driver-spi`, with no dependency on Spring or Kafka: interfaces and canonical DTOs
only.

```java
public interface MarketplaceDriver {

    ChannelType type();

    /** What this channel can do: guides publication-service in choosing commands. */
    DriverCapabilities capabilities();

    // ---------- outbound: catalog ----------
    UpsertResult upsertListing(ChannelContext ctx, ListingRequest request);
    UpdateResult updateInventory(ChannelContext ctx, List<InventoryUpdate> updates);
    UpdateResult updatePrice(ChannelContext ctx, List<PriceUpdate> updates);
    UpdateResult endListing(ChannelContext ctx, EndListingRequest request);

    // ---------- inbound: orders ----------
    OrderPage fetchOrders(ChannelContext ctx, OrderQuery query);
    Optional<RemoteOrder> fetchOrder(ChannelContext ctx, String channelOrderId);

    // ---------- reconciliation ----------
    ListingPage fetchListings(ChannelContext ctx, ListingQuery query);

    /** Translates a native error into the canonical taxonomy (see doc 09). */
    DriverError translate(Exception e);
}

public record DriverCapabilities(
    boolean supportsVariations,
    boolean supportsBulkInventory,   int maxBulkInventorySize,
    boolean supportsBulkUpsert,      int maxBulkUpsertSize,
    boolean supportsPartialUpdate,   // updating only the changed fields
    boolean supportsWebhooks,
    boolean supportsEndListing,      // versus zeroing the quantity
    boolean requiresCategoryMapping,
    Set<String> supportedImageFormats,
    int maxImages, int maxTitleLength
) {}
```

`ChannelContext` carries the `channelId`, resolved (already refreshed) credentials, locale, currency
and the `RateLimiter` to use. Implementations are **stateless and thread-safe**.

### 2.1 Shared contract tests
Module `piovra-driver-tck`: a JUnit 5 suite every driver must pass (upsert creates then updates;
inventory updates are idempotent; order fetching is paginated and ordered; errors translate
correctly; declared capabilities are honoured). It runs against a WireMock stub in the normal build
and, in a separate job, against the marketplace's **sandbox**.

## 3. eBay driver

### 3.1 Which APIs
The **Sell REST APIs** are the default choice; the legacy Trading API (XML) only where strictly
necessary.

| Function | API |
|---|---|
| Product / variant | **Inventory API** — `createOrReplaceInventoryItem`, `bulkCreateOrReplaceInventoryItem` (≤ 25), variant groups via `createOrReplaceInventoryItemGroup` |
| Offer (price, quantity, category, policies) | **Inventory API** — `createOffer` / `updateOffer` / `publishOffer` / `publishOfferByInventoryItemGroup` |
| Bulk stock and price | **Inventory API** — `bulkUpdatePriceQuantity` (≤ 25 offers) — the endpoint on the anti-oversell path |
| Ending a listing | `withdrawOffer` / `deleteOffer` (preferably: set the quantity to zero) |
| Orders | **Fulfillment API** — `getOrders` filtered by `lastmodifieddate` / `creationdate`, `limit`/`offset` pagination |
| Shipping | **Fulfillment API** — `createShippingFulfillment` |
| Policies (payment, shipping, returns) | **Account API** — referenced by id from the offer |
| Category and aspects | **Taxonomy API** — `getCategorySuggestions`, `getItemAspectsForCategory` |

**eBay's mental model**: `InventoryItem` (the product, keyed by **SKU**) → `Offer` (the listing on one
marketplace, with price/quantity/policies) → `publish` (which yields the `listingId`). The SKU as a
natural key is a gift: `createOrReplaceInventoryItem` is **already** an upsert.

### 3.2 Java client
- The historical official SDK (`ebay-sdk-java`) covers the **Trading APIs** and is dated.
- For the Sell APIs, eBay publishes **OpenAPI contracts**: we generate the clients with
  `openapi-generator-maven-plugin` (the `java` library, `okhttp-gson` or `native`) at build time,
  inside the `piovra-driver-ebay` module. Typed clients, refreshed by regenerating, with no abandoned
  dependency.

### 3.3 Authentication
OAuth 2.0 *authorization code* for user tokens. The access token lasts about 2 hours, the refresh
token about 18 months. An `EbayTokenManager` component:
- keeps refresh tokens in Vault (per `channelId`);
- refreshes the access token proactively (at 80 % of its lifetime) with a single refresh per cluster
  (a Redis lock) to avoid a thundering herd;
- treats `invalid_grant` as a **permanent error with an alert**: a human has to re-consent, and that
  must be said immediately.

### 3.4 Known pitfalls
- **Rate limits** apply per app and per API (daily quotas): the rate limiter must be keyed on
  `(appId, api)` and read the Developer Analytics API to learn the real quotas.
- **Required aspects** per category: when missing, the error is a validation failure. The driver must
  map canonical `attributes` to the category's aspects and report the missing ones explicitly (a
  `PERMANENT` error with code `MISSING_REQUIRED_ASPECT`, so the console can say what to add to the
  feed).
- **Images** must be referenced by public URL (or uploaded via EPS): the `media[].hash` diff avoids
  re-uploading.
- The **first publish** and subsequent updates are asynchronous on eBay's side: after `publishOffer`
  the real state has to be verified during reconciliation.
- The eBay **sandbox** is available and should be used in CI.

## 4. WooCommerce driver

### 4.1 API
REST API v3: `https://<store>/wp-json/wc/v3`.

| Function | Endpoint |
|---|---|
| Products | `GET/POST/PUT /products`, lookup by SKU: `GET /products?sku=…` |
| Bulk | `POST /products/batch` with `create` / `update` / `delete` (≤ 100 per call) |
| Variants | `/products/{id}/variations`, `POST /products/{id}/variations/batch` |
| Orders | `GET /orders?after=…&modified_after=…&per_page=100&orderby=modified` |
| Webhooks | `POST /webhooks` (`order.created`, `order.updated`, `product.updated`) |
| Stock | `stock_quantity`, `manage_stock`, `stock_status` fields on the product/variant |

### 4.2 Java client
There is no official Java SDK. We write a thin client on **Spring's `RestClient`/`WebClient`** (or
OkHttp) with Jackson — actually, on the JDK's own `HttpClient`, so the driver module stays free of
framework dependencies. The API is small and stable, and owning the client avoids an unmaintained
dependency. Auth: consumer key/secret via **Basic auth over HTTPS** (plain HTTP would require
OAuth 1.0a — configuration must forbid it).

### 4.3 Upsert on Woo
Woo has no writable natural key: we create with `POST` and update with `PUT /products/{id}`, so the
`external_id` is required. Strategy:
1. `channel_listing.external_id` present ⇒ `PUT` directly.
2. Absent ⇒ `GET /products?sku={sku}` (lookup-before-create) ⇒ adopt the id if found, otherwise
   `POST`.
3. Always store the returned id **before** treating the command as successful.

For variable products: the parent is `type=variable` with `attributes[].variation=true`, children are
variations with their own SKU; `external_variant_ids` holds the `canonicalSku → variationId` map.

### 4.4 Known pitfalls
- **Performance**: WordPress is the bottleneck. Always use `/batch`, `per_page=100`, and the `_fields`
  parameter to shrink the response payload.
- **Order pagination**: page by ascending `modified` with a time cursor, not by page number (pages
  shift when new orders arrive during the scan).
- **Timeouts and 502/504**: common on shared hosting. A `POST /products/batch` that timed out **may
  still have executed**: the retry must be preceded by a lookup by SKU, never by a blind create.
- **Webhooks**: verify the `X-WC-Webhook-Signature` (base64 HMAC-SHA256 of the body with the secret);
  Woo disables webhooks automatically after repeated failures, so a periodic health check must
  re-register them.
- **Time zones**: orders may be expressed in the site's local time; use `dates_are_gmt=true` and
  normalize everything to UTC.

## 5. Adding a new marketplace

1. A new `piovra-driver-<x>` module implementing `MarketplaceDriver`.
2. Declare the real `DriverCapabilities` (without optimism: they guide the core).
3. Map the taxonomy and the native errors onto the canonical taxonomy.
4. Make the TCK pass.
5. A new `connector-<x>` service (normally a copy of the template: consumers, scheduler, rate limiter,
   configuration).
6. Dedicated topics and an entry in `channel-config`.

No changes to `catalog`, `inventory`, `order` or `publication`. If they need changing, the SPI is
wrong.
