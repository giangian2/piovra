package dev.piovra.connector.woocommerce.adapter.in.scheduling;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.piovra.connector.woocommerce.application.port.out.ChannelDefinitionCache;
import dev.piovra.connector.woocommerce.application.service.OrderPollingService;
import dev.piovra.connector.woocommerce.config.ConnectorProperties;
import dev.piovra.model.channel.ChannelDefinition;

/**
 * The clock that drives order acquisition.
 *
 * <p>Sequential over channels: parallelism across replicas already comes from the per-row lease in
 * {@code poll_cursor}, and a fan-out here would buy nothing until there are dozens of stores. The
 * {@link ReentrantLock} is the same guard {@code OutboxRelay} uses - it keeps a slow tick from
 * overlapping the next one, and {@code tryLock} over {@code synchronized} is the project's rule
 * (docs/12-development-guidelines.md section 5.3).
 *
 * <p>{@code @EnableScheduling} arrives with {@code piovra-outbox}, which every writing module has.
 */
@Component
public class OrderPollScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderPollScheduler.class);

    private final ChannelDefinitionCache channels;
    private final OrderPollingService pollingService;
    private final ConnectorProperties properties;
    private final ReentrantLock lock = new ReentrantLock();

    public OrderPollScheduler(
            ChannelDefinitionCache channels, OrderPollingService pollingService, ConnectorProperties properties) {
        this.channels = channels;
        this.pollingService = pollingService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${piovra.connector.polling.orders:PT2M}")
    public void pollOrders() {
        if (!lock.tryLock()) {
            log.debug("the previous tick is still running, skipping this one");
            return;
        }
        try {
            for (ChannelDefinition channel : channels.enabledChannelsOfType(properties.channelType())) {
                pollingService.pollOnce(channel);
            }
        } finally {
            lock.unlock();
        }
    }
}
