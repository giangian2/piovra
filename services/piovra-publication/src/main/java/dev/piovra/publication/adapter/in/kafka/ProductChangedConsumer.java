package dev.piovra.publication.adapter.in.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import dev.piovra.events.ProductChanged;
import dev.piovra.events.Topics;
import dev.piovra.publication.application.service.ProductChangedHandler;

// Named explicitly: three modules each consume ProductChanged, and in the combined piovra-core
// deployable the default bean name (the class's simple name) is the same for all three. Same
// convention as the per-module outbox beans.
@Component("publicationProductChangedConsumer")
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
