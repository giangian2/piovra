package dev.piovra.order.adapter.in.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import dev.piovra.events.ProductChanged;
import dev.piovra.events.Topics;
import dev.piovra.order.application.service.KnownSkuInitializer;

@Component
public class ProductChangedConsumer {

    private final KnownSkuInitializer initializer;

    public ProductChangedConsumer(KnownSkuInitializer initializer) {
        this.initializer = initializer;
    }

    @KafkaListener(topics = Topics.CATALOG_PRODUCT_CHANGED)
    public void onMessage(ProductChanged event, Acknowledgment ack) {
        initializer.handle(event);
        ack.acknowledge();
    }
}
