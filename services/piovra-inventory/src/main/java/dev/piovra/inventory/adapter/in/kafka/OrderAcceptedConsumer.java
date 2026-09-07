package dev.piovra.inventory.adapter.in.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import dev.piovra.events.OrderAccepted;
import dev.piovra.events.Topics;
import dev.piovra.inventory.application.service.OrderAcceptedHandler;

@Component
public class OrderAcceptedConsumer {

    private final OrderAcceptedHandler handler;

    public OrderAcceptedConsumer(OrderAcceptedHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(topics = Topics.ORDER_ACCEPTED)
    public void onMessage(OrderAccepted event, Acknowledgment ack) {
        handler.handle(event);
        ack.acknowledge();
    }
}
