package dev.piovra.connector.woocommerce.config;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import dev.piovra.connector.woocommerce.adapter.out.ratelimit.InProcessRateLimiter;
import dev.piovra.connector.woocommerce.application.port.out.CredentialsResolver;
import dev.piovra.driver.spi.ChannelContext;
import dev.piovra.driver.spi.RateLimiter;
import dev.piovra.model.channel.ChannelDefinition;

/**
 * Builds the per-call context the driver needs. The driver is stateless and serves every account;
 * everything that is per-account - credentials, the rate-limit bucket - lives here, keyed by
 * channelId (docs/08-marketplace-drivers.md section 1.1).
 */
@Component
public class ChannelContextFactory {

    private final CredentialsResolver credentialsResolver;
    private final ConnectorProperties properties;
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    public ChannelContextFactory(CredentialsResolver credentialsResolver, ConnectorProperties properties) {
        this.credentialsResolver = credentialsResolver;
        this.properties = properties;
    }

    public ChannelContext create(ChannelDefinition definition) {
        return new ChannelContext(
                definition.tenantId(),
                definition.channelId(),
                definition.marketplaceCode(),
                Locale.ITALY,
                "EUR",
                credentialsResolver.resolve(definition),
                definition.settings(),
                limiterFor(definition));
    }

    /** One bucket per account: two stores must not throttle each other. */
    private RateLimiter limiterFor(ChannelDefinition definition) {
        return limiters.computeIfAbsent(
                definition.channelId().value(),
                key -> new InProcessRateLimiter(
                        properties.rateLimit().requestsPerSecond(),
                        properties.rateLimit().burst()));
    }
}
