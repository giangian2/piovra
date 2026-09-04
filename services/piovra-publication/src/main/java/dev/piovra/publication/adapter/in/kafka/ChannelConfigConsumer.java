package dev.piovra.publication.adapter.in.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import dev.piovra.events.ChannelConfigChanged;
import dev.piovra.events.Topics;
import dev.piovra.publication.application.service.ChannelConfigCacheUpdater;

@Component
public class ChannelConfigConsumer {

    private final ChannelConfigCacheUpdater cacheUpdater;

    public ChannelConfigConsumer(ChannelConfigCacheUpdater cacheUpdater) {
        this.cacheUpdater = cacheUpdater;
    }

    @KafkaListener(topics = Topics.CHANNEL_CONFIG)
    public void onMessage(ChannelConfigChanged event, Acknowledgment ack) {
        cacheUpdater.handle(event);
        ack.acknowledge();
    }
}
