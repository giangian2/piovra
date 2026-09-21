package dev.piovra.connector.woocommerce.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import dev.piovra.model.channel.ChannelType;

/**
 * Binds {@code piovra.connector.*}, the keys the connector's application.yml has been carrying since
 * the module was scaffolded with nothing to read them.
 *
 * @param instanceId who holds a poll lease. Defaults to the hostname in the yml, so two replicas of
 *     the same image never claim to be each other.
 * @param credentials consumer key/secret per channelId. A stand-in for Vault, which is not in the
 *     stack yet (docs/08-marketplace-drivers.md section 3.3).
 */
@ConfigurationProperties(prefix = "piovra.connector")
public record ConnectorProperties(
        ChannelType channelType,
        String instanceId,
        Polling polling,
        RateLimit rateLimit,
        Map<String, Credentials> credentials) {

    public ConnectorProperties {
        channelType = channelType == null ? ChannelType.WOOCOMMERCE : channelType;
        instanceId = instanceId == null || instanceId.isBlank() ? "local" : instanceId;
        polling = polling == null ? Polling.DEFAULT : polling;
        rateLimit = rateLimit == null ? RateLimit.DEFAULT : rateLimit;
        credentials = credentials == null ? Map.of() : Map.copyOf(credentials);
    }

    /**
     * @param overlap the sliding window reaches back this far before the cursor. It absorbs the
     *     marketplace's visibility lag and clock skew; the duplicates it produces are harmless,
     *     because {@code (channelId, channelOrderId)} is UNIQUE downstream.
     * @param lease how long a replica may hold a channel. Comfortably above a full multi-page scan,
     *     and renewed on every committed page.
     * @param maxPagesPerTick stops one very stale channel from starving the others inside a tick.
     */
    public record Polling(Duration orders, Duration overlap, Duration lease, int pageSize, int maxPagesPerTick) {

        static final Polling DEFAULT = new Polling(null, null, null, 0, 0);

        public Polling {
            orders = orders == null ? Duration.ofMinutes(2) : orders;
            overlap = overlap == null ? Duration.ofMinutes(5) : overlap;
            lease = lease == null ? Duration.ofMinutes(5) : lease;
            pageSize = pageSize <= 0 ? 100 : pageSize;
            maxPagesPerTick = maxPagesPerTick <= 0 ? 20 : maxPagesPerTick;
        }
    }

    /** Per-account budget towards the marketplace, not per replica: see {@code InProcessRateLimiter}. */
    public record RateLimit(double requestsPerSecond, int burst) {

        static final RateLimit DEFAULT = new RateLimit(0, 0);

        public RateLimit {
            requestsPerSecond = requestsPerSecond <= 0 ? 8 : requestsPerSecond;
            burst = burst <= 0 ? 20 : burst;
        }
    }

    public record Credentials(String consumerKey, String consumerSecret) {}
}
