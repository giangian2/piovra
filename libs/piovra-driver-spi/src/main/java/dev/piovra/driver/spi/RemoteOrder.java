package dev.piovra.driver.spi;

import dev.piovra.model.order.CanonicalOrder;

/**
 * An order as the driver returns it: already normalized into the canonical model, but with SKUs
 * <b>not yet resolved</b> (resolution belongs to the order service, which owns the mapping) and
 * with the original payload kept for debugging the mappings.
 *
 * @param rawPayload the marketplace's original JSON. It never reaches the logs; it goes to object
 *     storage or to a column with restricted access, because it contains personal data.
 */
public record RemoteOrder(CanonicalOrder order, String rawPayload) {}
