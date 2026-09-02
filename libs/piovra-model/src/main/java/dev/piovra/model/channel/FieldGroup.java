package dev.piovra.model.channel;

/**
 * Groups of fields the diff is computed over (docs/06-publish-flow.md).
 *
 * <p>The grouping is not cosmetic: it determines WHICH API call the driver will make. Changing only
 * the stock must cost a lightweight call, not a full listing update.
 */
public enum FieldGroup {
    /** Available quantity. High frequency, dedicated endpoint, high priority. */
    STOCK,
    /** Price and compare-at price. */
    PRICE,
    /** Title, description, attributes, category. Heavy listing update. */
    CONTENT,
    /** Images. The most expensive call: compared by hash, never by URL. */
    MEDIA,
    /** Weight, dimensions, shipping policy. */
    SHIPPING
}
