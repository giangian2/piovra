package dev.piovra.order.adapter.in.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import dev.piovra.events.OrderReceived;
import dev.piovra.events.Topics;
import dev.piovra.order.application.port.in.IngestOrderUseCase;

/** Inert today: no driver publishes {@code channel.order.received} yet (eBay/WooCommerce drivers
 * are phase-3 stubs). Wired now so nothing needs to change here once one does. */
@Component
public class ChannelOrderReceivedConsumer {

    private final IngestOrderUseCase ingestOrderUseCase;

    public ChannelOrderReceivedConsumer(IngestOrderUseCase ingestOrderUseCase) {
        this.ingestOrderUseCase = ingestOrderUseCase;
    }

    @KafkaListener(topics = Topics.CHANNEL_ORDER_RECEIVED)
    public void onMessage(OrderReceived event, Acknowledgment ack) {
        ingestOrderUseCase.ingest(event.order(), event.rawPayloadUri());
        ack.acknowledge();
    }
}
