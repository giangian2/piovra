package dev.piovra.publication.adapter.in.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import dev.piovra.events.ProductChanged;
import dev.piovra.events.Topics;
import dev.piovra.publication.application.service.ProductChangedHandler;

@Component
public class ProductChangedConsumer {

    private final ProductChangedHandler handler;

    public ProductChangedConsumer(ProductChangedHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(topics = Topics.CATALOG_PRODUCT_CHANGED)
    public void onMessage(ProductChanged event, Acknowledgment ack) {
        handler.handle(event);
        ack.acknowledge();
    }
}
