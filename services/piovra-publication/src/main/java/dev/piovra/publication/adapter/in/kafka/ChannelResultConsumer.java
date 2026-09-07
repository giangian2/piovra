package dev.piovra.publication.adapter.in.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import dev.piovra.events.ChannelResult;
import dev.piovra.events.Topics;
import dev.piovra.publication.application.service.ChannelResultHandler;

@Component
public class ChannelResultConsumer {

    private final ChannelResultHandler handler;

    public ChannelResultConsumer(ChannelResultHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(topics = Topics.CHANNEL_RESULT)
    public void onMessage(ChannelResult event, Acknowledgment ack) {
        handler.handle(event);
        ack.acknowledge();
    }
}
