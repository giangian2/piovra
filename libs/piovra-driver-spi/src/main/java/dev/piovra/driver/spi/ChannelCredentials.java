package dev.piovra.driver.spi;

/**
 * Credentials already resolved from Vault. Does not implement a meaningful toString(): a secret in
 * a log is a security incident, not an inconvenience.
 */
public final class ChannelCredentials {

    private final String accessToken;
    private final String consumerKey;
    private final String consumerSecret;

    private ChannelCredentials(String accessToken, String consumerKey, String consumerSecret) {
        this.accessToken = accessToken;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
    }

    /** OAuth 2.0 bearer token, already refreshed by the connector (eBay). */
    public static ChannelCredentials bearer(String accessToken) {
        return new ChannelCredentials(accessToken, null, null);
    }

    /** Basic auth with consumer key/secret over HTTPS (WooCommerce). */
    public static ChannelCredentials basic(String key, String secret) {
        return new ChannelCredentials(null, key, secret);
    }

    public String accessToken() {
        return accessToken;
    }

    public String consumerKey() {
        return consumerKey;
    }

    public String consumerSecret() {
        return consumerSecret;
    }

    @Override
    public String toString() {
        return "ChannelCredentials[***]";
    }
}
