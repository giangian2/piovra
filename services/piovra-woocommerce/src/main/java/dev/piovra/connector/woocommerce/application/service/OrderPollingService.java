package dev.piovra.connector.woocommerce.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import dev.piovra.connector.woocommerce.application.port.out.PollCursorStore;
import dev.piovra.connector.woocommerce.application.port.out.PollKind;
import dev.piovra.connector.woocommerce.application.port.out.PollLease;
import dev.piovra.connector.woocommerce.application.port.out.RawPayloadStore;
import dev.piovra.connector.woocommerce.config.ChannelContextFactory;
import dev.piovra.connector.woocommerce.config.ConnectorProperties;
import dev.piovra.connector.woocommerce.domain.PollWindow;
import dev.piovra.crosscutting.MdcKeys;
import dev.piovra.driver.spi.ChannelContext;
import dev.piovra.driver.spi.MarketplaceDriver;
import dev.piovra.driver.spi.OrderPage;
import dev.piovra.driver.spi.OrderQuery;
import dev.piovra.driver.spi.RemoteOrder;
import dev.piovra.model.channel.ChannelDefinition;

/**
 * One polling pass over one channel.
 *
 * <p>The transaction boundaries are the design. The marketplace call and the object-storage writes
 * happen with <b>no transaction open</b> - a slow store must never hold a database connection
 * (docs/12-development-guidelines.md section 5.4) - and each page is then committed in one short
 * transaction by {@link OrderPageCommitter}.
 */
@Service
public class OrderPollingService {

    private static final Logger log = LoggerFactory.getLogger(OrderPollingService.class);

    private final MarketplaceOrderFetcher fetcher;
    private final MarketplaceDriver driver;
    private final RawPayloadStore rawPayloadStore;
    private final PollCursorStore cursorStore;
    private final OrderPageCommitter committer;
    private final ChannelContextFactory contextFactory;
    private final ConnectorProperties properties;

    public OrderPollingService(
            MarketplaceOrderFetcher fetcher,
            MarketplaceDriver driver,
            RawPayloadStore rawPayloadStore,
            PollCursorStore cursorStore,
            OrderPageCommitter committer,
            ChannelContextFactory contextFactory,
            ConnectorProperties properties) {
        this.fetcher = fetcher;
        this.driver = driver;
        this.rawPayloadStore = rawPayloadStore;
        this.cursorStore = cursorStore;
        this.committer = committer;
        this.contextFactory = contextFactory;
        this.properties = properties;
    }

    /** Does nothing at all when another replica already holds this channel. */
    public void pollOnce(ChannelDefinition channel) {
        Optional<PollLease> claimed = cursorStore.acquire(
                channel.tenantId(),
                channel.channelId(),
                PollKind.ORDERS,
                properties.polling().lease());
        if (claimed.isEmpty()) {
            return;
        }

        PollLease lease = claimed.get();
        MDC.put(MdcKeys.TENANT, channel.tenantId().value());
        MDC.put(MdcKeys.CHANNEL, channel.channelId().value());
        String errorCode = null;
        try {
            scan(channel, lease);
        } catch (Exception e) {
            // The cursor is untouched, so the next tick re-reads the same window. Translating here
            // rather than rethrowing keeps one bad channel from stopping the others.
            errorCode = driver.translate(e).code();
            log.warn("order polling failed: code={}", errorCode, e);
        } finally {
            cursorStore.release(lease, errorCode);
            MDC.remove(MdcKeys.TENANT);
            MDC.remove(MdcKeys.CHANNEL);
        }
    }

    private void scan(ChannelDefinition channel, PollLease lease) {
        ChannelContext context = contextFactory.create(channel);
        PollWindow window = PollWindow.of(lease.cursorAt(), properties.polling().overlap(), Instant.now());
        OrderQuery query = window.toOrderQuery(properties.polling().pageSize(), lease.pageCursor());

        for (int page = 0; page < properties.polling().maxPagesPerTick(); page++) {
            OrderPage fetched = fetcher.fetch(context, query);
            List<StoredOrder> stored = archive(channel, fetched);

            boolean finished = !fetched.hasMore();
            // While a scan is in flight only the driver's own token moves; the window's start only
            // advances once the scan has actually reached the end, so a crash resumes mid-scan
            // instead of skipping whatever it had not read yet.
            committer.commit(
                    lease,
                    stored,
                    finished ? window.to() : lease.cursorAt(),
                    finished ? null : fetched.nextCursor(),
                    properties.polling().lease());

            log.debug("polled {} orders, finished={}", stored.size(), finished);
            if (finished) {
                return;
            }
            query = query.withCursor(fetched.nextCursor());
        }
        log.info("page budget exhausted, the channel resumes from its cursor on the next tick");
    }

    /**
     * The payload is archived before the event is written, never after: an event pointing at an
     * object that does not exist yet would be a dangling reference on a topic others read.
     */
    private List<StoredOrder> archive(ChannelDefinition channel, OrderPage page) {
        List<StoredOrder> stored = new ArrayList<>();
        for (RemoteOrder remote : page.orders()) {
            String uri = rawPayloadStore.store(
                    channel.tenantId(), channel.channelId(), remote.order().channelOrderId(), remote.rawPayload());
            stored.add(new StoredOrder(remote.order(), uri));
        }
        return stored;
    }
}
