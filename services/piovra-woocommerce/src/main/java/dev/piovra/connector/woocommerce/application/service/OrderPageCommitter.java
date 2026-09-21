package dev.piovra.connector.woocommerce.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.piovra.connector.woocommerce.application.port.out.PollCursorStore;
import dev.piovra.connector.woocommerce.application.port.out.PollLease;
import dev.piovra.events.OrderReceived;
import dev.piovra.outbox.OutboxWriter;

/**
 * Writes one page of orders and the cursor that says they were read, in a single transaction.
 *
 * <p>This is the whole point of the design: if the cursor advanced and the orders had not been
 * recorded, those orders would be lost for good - a marketplace does not re-offer what has fallen
 * out of the polling window. The reverse, recording orders and not advancing, merely re-reads a
 * page, and the duplicates die on the order service's UNIQUE (channelId, channelOrderId).
 *
 * <p>A separate bean from {@code OrderPollingService} on purpose: a {@code @Transactional} method
 * called from within the same class would go through no proxy and open no transaction
 * (docs/12-development-guidelines.md section 3.3).
 */
@Component
public class OrderPageCommitter {

    private final OutboxWriter outboxWriter;
    private final PollCursorStore cursorStore;

    public OrderPageCommitter(@Qualifier("wooOutboxWriter") OutboxWriter outboxWriter, PollCursorStore cursorStore) {
        this.outboxWriter = outboxWriter;
        this.cursorStore = cursorStore;
    }

    @Transactional
    public void commit(
            PollLease lease, List<StoredOrder> orders, Instant cursorAt, String pageCursor, Duration leaseExtension) {
        for (StoredOrder stored : orders) {
            outboxWriter.append(OrderReceived.of(stored.order(), stored.rawPayloadUri()));
        }
        cursorStore.advance(lease, cursorAt, pageCursor, leaseExtension);
    }
}
