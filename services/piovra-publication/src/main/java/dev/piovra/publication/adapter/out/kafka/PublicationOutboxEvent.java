package dev.piovra.publication.adapter.out.kafka;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import dev.piovra.outbox.JpaOutboxWriter.OutboxRowData;
import dev.piovra.outbox.OutboxEntity;

@Entity
@Table(schema = "publication", name = "outbox_event")
public class PublicationOutboxEvent extends OutboxEntity {

    protected PublicationOutboxEvent() {}

    public PublicationOutboxEvent(OutboxRowData row) {
        super(row.id(), row.partitionKey(), row.topic(), row.eventType(), row.payload(), row.headers());
    }
}
