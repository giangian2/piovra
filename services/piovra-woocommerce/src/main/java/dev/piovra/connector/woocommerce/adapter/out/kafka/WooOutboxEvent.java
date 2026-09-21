package dev.piovra.connector.woocommerce.adapter.out.kafka;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import dev.piovra.outbox.JpaOutboxWriter.OutboxRowData;
import dev.piovra.outbox.OutboxEntity;

@Entity
@Table(schema = "connector_woocommerce", name = "outbox_event")
public class WooOutboxEvent extends OutboxEntity {

    protected WooOutboxEvent() {}

    public WooOutboxEvent(OutboxRowData row) {
        super(row.id(), row.partitionKey(), row.topic(), row.eventType(), row.payload(), row.headers());
    }
}
