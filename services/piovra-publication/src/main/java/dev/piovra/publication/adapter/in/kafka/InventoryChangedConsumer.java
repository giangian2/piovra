package dev.piovra.publication.adapter.in.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import dev.piovra.events.InventoryChanged;
import dev.piovra.events.Topics;
import dev.piovra.publication.application.service.InventoryCacheUpdater;

@Component
public class InventoryChangedConsumer {

    private final InventoryCacheUpdater cacheUpdater;

    public InventoryChangedConsumer(InventoryCacheUpdater cacheUpdater) {
        this.cacheUpdater = cacheUpdater;
    }

    @KafkaListener(topics = Topics.INVENTORY_CHANGED)
    public void onMessage(InventoryChanged event, Acknowledgment ack) {
        cacheUpdater.handle(event);
        ack.acknowledge();
    }
}
