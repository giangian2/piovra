package dev.piovra.catalog.adapter.out.kafka;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import dev.piovra.outbox.JpaOutboxWriter.OutboxRowData;
import dev.piovra.outbox.OutboxEntity;

@Entity
@Table(schema = "catalog", name = "outbox_event")
public class CatalogOutboxEvent extends OutboxEntity {

    protected CatalogOutboxEvent() {}

    public CatalogOutboxEvent(OutboxRowData row) {
        super(row.id(), row.partitionKey(), row.topic(), row.eventType(), row.payload(), row.headers());
    }
}
