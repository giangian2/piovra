package dev.piovra.driver.woocommerce;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import dev.piovra.common.ErrorClass;
import dev.piovra.driver.spi.ChannelContext;
import dev.piovra.driver.spi.DriverError;
import dev.piovra.driver.spi.DriverException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * HTTP client for the WooCommerce v3 API.
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

    private static final JsonMapper JSON = JsonMapper.builder().build();

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

    /** A parsed response plus the headers, because orders paging needs {@code X-WP-Total}. */
    record WooResponse(int status, JsonNode body, HttpResponse<String> raw) {

        Optional<Integer> intHeader(String name) {
            return raw.headers().firstValue(name).map(Integer::parseInt);
        }
    }

    /**
     * @throws WooApiException on any non-2xx, carrying WooCommerce's own error code
     */
    WooResponse get(ChannelContext ctx, String path, Map<String, String> query) {
        acquirePermit(ctx);
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl(ctx) + path + queryString(query)))
                .timeout(requestTimeout)
                .header("Authorization", basicAuth(ctx))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DriverException(
                    DriverError.of(ErrorClass.TRANSIENT, "WOO_TRANSIENT_INTERRUPTED", "call interrupted"), e);
        } catch (java.io.IOException e) {
            throw new DriverException(
                    DriverError.of(ErrorClass.TRANSIENT, "WOO_TRANSIENT_IO", "cannot reach the store"), e);
        }

        if (response.statusCode() >= 300) {
            throw errorFor(ctx, response);
        }
        return new WooResponse(response.statusCode(), JSON.readTree(response.body()), response);
    }

    /**
     * The rate limiter is the connector's, shared by every call to this channel: the driver consults
     * it, it does not own it.
     */
    private void acquirePermit(ChannelContext ctx) {
        try {
            if (!ctx.rateLimiter().acquire(requestTimeout)) {
                throw new DriverException(DriverError.of(
                        ErrorClass.RATE_LIMIT, "WOO_RATE_LIMIT_LOCAL", "no rate-limit permit within the timeout"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DriverException(
                    DriverError.of(ErrorClass.TRANSIENT, "WOO_TRANSIENT_INTERRUPTED", "interrupted while waiting"), e);
        }
    }

    /**
     * WooCommerce reports errors as {@code {"code","message","data":{"status"}}}. The native code is
     * what {@link WooCommerceDriver#translate} classifies on - never the HTTP status alone.
     */
    private WooApiException errorFor(ChannelContext ctx, HttpResponse<String> response) {
        String code = "woocommerce_unknown_error";
        String message = "HTTP " + response.statusCode();
        try {
            JsonNode error = JSON.readTree(response.body());
            code = error.path("code").asString(code);
            message = error.path("message").asString(message);
        } catch (RuntimeException ignored) {
            // A WordPress error page is HTML, not JSON. The status is the only signal left.
        }

        Duration retryAfter = response.headers()
                .firstValue("Retry-After")
                .map(value -> Duration.ofSeconds(Long.parseLong(value.trim())))
                .orElse(null);
        if (response.statusCode() == 429 && retryAfter != null) {
            ctx.rateLimiter().penalize(retryAfter);
        }
        return new WooApiException(response.statusCode(), code, message, retryAfter);
    }

    private static String basicAuth(ChannelContext ctx) {
        String pair = ctx.credentials().consumerKey() + ":" + ctx.credentials().consumerSecret();
        return "Basic " + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
    }

    private static String queryString(Map<String, String> query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("&", "?", "");
        query.forEach((key, value) -> joiner.add(encode(key) + "=" + encode(value)));
        return joiner.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
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
}
