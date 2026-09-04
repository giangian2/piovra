package dev.piovra.channelconfig.adapter.out.kafka;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import dev.piovra.outbox.JpaOutboxWriter.OutboxRowData;
import dev.piovra.outbox.OutboxEntity;

@Entity
@Table(schema = "channel_config", name = "outbox_event")
public class ChannelConfigOutboxEvent extends OutboxEntity {

    protected ChannelConfigOutboxEvent() {}

    public ChannelConfigOutboxEvent(OutboxRowData row) {
        super(row.id(), row.partitionKey(), row.topic(), row.eventType(), row.payload(), row.headers());
    }
}
