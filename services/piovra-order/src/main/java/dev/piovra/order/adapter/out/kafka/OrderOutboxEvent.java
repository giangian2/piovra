package dev.piovra.order.adapter.out.kafka;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import dev.piovra.outbox.JpaOutboxWriter.OutboxRowData;
import dev.piovra.outbox.OutboxEntity;

@Entity
@Table(schema = "orders", name = "outbox_event")
public class OrderOutboxEvent extends OutboxEntity {

    protected OrderOutboxEvent() {}

    public OrderOutboxEvent(OutboxRowData row) {
        super(row.id(), row.partitionKey(), row.topic(), row.eventType(), row.payload(), row.headers());
    }
}
