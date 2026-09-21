package dev.piovra.connector.woocommerce.application.service;

import dev.piovra.model.order.CanonicalOrder;

/** An order the driver translated, plus where its original payload was archived. */
public record StoredOrder(CanonicalOrder order, String rawPayloadUri) {}
