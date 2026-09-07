package dev.piovra.order.application.port.in;

import dev.piovra.model.order.CanonicalOrder;

public interface IngestOrderUseCase {

    /**
     * Dedups on {@code (tenantId, channelId, channelOrderId)}: a resend of an already-known order
     * with the same status is a no-op; a resend with a different status updates it and re-emits
     * {@code OrderAccepted} so inventory can apply the resulting movement (docs/07-order-flow.md
     * section 3). {@code incomingOrder}'s lines are expected unresolved (UNMAPPED, sku null) - this
     * resolves them.
     *
     * @return the order's current persisted state after processing (new, updated, or unchanged)
     */
    CanonicalOrder ingest(CanonicalOrder incomingOrder, String rawPayloadUri);
}
