package dev.piovra.driver.spi;

import java.util.Locale;
import java.util.Map;

import dev.piovra.common.ChannelId;
import dev.piovra.common.TenantId;

/**
 * Everything specific to the account the driver is operating on. Credentials arrive already
 * resolved and already refreshed: token renewal is the connector's job, not the driver's.
 */
public record ChannelContext(
        TenantId tenantId,
        ChannelId channelId,
        /** For example "EBAY_IT" on eBay, the store URL on WooCommerce. */
        String marketplaceCode,
        Locale locale,
        String currencyCode,
        ChannelCredentials credentials,
        /** Per-channel free-form parameters: eBay policy ids, prefixes, flags. */
        Map<String, String> settings,
        /** The driver consults it BEFORE every external call. The implementation belongs to the
         * connector. */
        RateLimiter rateLimiter) {

    public ChannelContext {
        settings = settings == null ? Map.of() : Map.copyOf(settings);
    }

    public String setting(String key, String defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }
}
