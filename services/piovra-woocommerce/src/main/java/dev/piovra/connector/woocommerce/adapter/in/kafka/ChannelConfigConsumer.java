package dev.piovra.connector.woocommerce.adapter.in.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import dev.piovra.connector.woocommerce.application.service.ChannelConfigCacheUpdater;
import dev.piovra.events.ChannelConfigChanged;
import dev.piovra.events.Topics;

// Named explicitly: piovra-publication declares a ChannelConfigConsumer too, and the default bean
// name is the class's simple name. They live in different deployables today, but that is exactly
// how the three ProductChangedConsumer classes broke the combined context.
@Component("wooChannelConfigConsumer")
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
