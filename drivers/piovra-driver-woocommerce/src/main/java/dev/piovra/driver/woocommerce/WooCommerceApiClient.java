package dev.piovra.driver.woocommerce;

import java.net.http.HttpClient;
import java.time.Duration;

import dev.piovra.driver.spi.ChannelContext;

/**
 * HTTP client for the WooCommerce v3 API.
 *
 * <p><b>Status: skeleton.</b>
 *
 * <p>Built on the JDK's {@link java.net.http.HttpClient} rather than Spring's RestClient, so the
 * driver module stays free of framework dependencies and remains usable from a test, a CLI or a
 * batch job. With virtual threads, blocking code sustains all the concurrency we need, so there is
 * no reason to complicate it.
 *
 * <p>Auth: Basic with consumer key/secret over HTTPS. Plain HTTP would require OAuth 1.0a:
 * configuration must forbid unencrypted endpoints.
 */
public class WooCommerceApiClient {

    private final HttpClient http;
    private final Duration requestTimeout;

    public WooCommerceApiClient(HttpClient http, Duration requestTimeout) {
        this.http = http;
        this.requestTimeout = requestTimeout;
    }

    public static WooCommerceApiClient withDefaults() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        // Generous read timeout: /products/batch on shared hosting is slow.
        return new WooCommerceApiClient(client, Duration.ofSeconds(60));
    }

    /** Base API URL for the store in the given context. */
    String baseUrl(ChannelContext ctx) {
        String store = ctx.marketplaceCode();
        if (!store.startsWith("https://")) {
            throw new IllegalArgumentException("WooCommerce requires HTTPS: " + store);
        }
        return store.replaceAll("/+$", "") + "/wp-json/wc/v3";
    }

    HttpClient http() {
        return http;
    }

    Duration requestTimeout() {
        return requestTimeout;
    }

    // TODO phase 1: typed get/post/put, handling of X-WP-Total, _fields and retry-after, and
    //  translation of error responses into WooApiException.
}
