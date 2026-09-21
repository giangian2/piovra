package dev.piovra.driver.woocommerce;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import dev.piovra.common.ChannelId;
import dev.piovra.common.ErrorClass;
import dev.piovra.common.TenantId;
import dev.piovra.driver.spi.ChannelContext;
import dev.piovra.driver.spi.ChannelCredentials;
import dev.piovra.driver.spi.DriverError;
import dev.piovra.driver.spi.OrderPage;
import dev.piovra.driver.spi.OrderQuery;
import dev.piovra.driver.spi.RateLimiter;
import dev.piovra.driver.spi.RemoteOrder;

/**
 * Exercises the driver against a WireMock store over <b>HTTPS</b>, because
 * {@code WooCommerceApiClient.baseUrl} refuses plain HTTP - the consumer key and secret travel in an
 * Authorization header, and that rule is worth keeping out of the test's convenience.
 */
class WooCommerceOrderFetchTest {

    static {
        // WireMock serves a self-signed certificate for "localhost"; the JDK client would reject the
        // hostname. Test-only, and set before any HttpClient in this JVM exists.
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
    }

    private static WireMockServer wireMock;

    private final RecordingRateLimiter rateLimiter = new RecordingRateLimiter();
    private final WooCommerceDriver driver = new WooCommerceDriver(trustingClient());

    @BeforeAll
    static void startServer() {
        wireMock = new WireMockServer(options().dynamicPort().dynamicHttpsPort());
        wireMock.start();
    }

    @AfterAll
    static void stopServer() {
        wireMock.stop();
    }

    @Test
    void fetch_orders_asks_for_gmt_dates_ordered_by_modified_ascending() {
        stubOrders("[]");

        driver.fetchOrders(context(), OrderQuery.since(Instant.parse("2026-09-21T10:00:00Z"), 2));

        wireMock.verify(getRequestedFor(urlPathEqualTo("/wp-json/wc/v3/orders"))
                .withQueryParam("dates_are_gmt", WireMock.equalTo("true"))
                .withQueryParam("orderby", WireMock.equalTo("modified"))
                .withQueryParam("order", WireMock.equalTo("asc"))
                .withQueryParam("modified_after", WireMock.equalTo("2026-09-21T10:00:00")));
    }

    @Test
    void a_full_page_returns_a_cursor_past_the_last_modified_order() {
        stubOrders(pageOf("2026-09-21T10:10:00", "2026-09-21T10:20:00"));

        OrderPage page = driver.fetchOrders(context(), OrderQuery.since(Instant.parse("2026-09-21T10:00:00Z"), 2));

        assertThat(page.hasMore()).isTrue();
        assertThat(WooOrderCursor.parse(page.nextCursor(), Instant.EPOCH))
                .isEqualTo(new WooOrderCursor(Instant.parse("2026-09-21T10:20:00Z"), 1));
    }

    @Test
    void a_partial_page_returns_no_cursor() {
        stubOrders(pageOf("2026-09-21T10:10:00"));

        OrderPage page = driver.fetchOrders(context(), OrderQuery.since(Instant.parse("2026-09-21T10:00:00Z"), 2));

        assertThat(page.hasMore()).isFalse();
        assertThat(page.orders()).hasSize(1);
    }

    @Test
    void orders_sharing_one_modified_timestamp_advance_by_page_instead_of_looping() {
        // Every order on a full page carries the cursor's own second: a pure time cursor would ask
        // the same question forever.
        stubOrders(pageOf("2026-09-21T10:00:00", "2026-09-21T10:00:00"));

        OrderPage page = driver.fetchOrders(context(), OrderQuery.since(Instant.parse("2026-09-21T10:00:00Z"), 2));

        assertThat(WooOrderCursor.parse(page.nextCursor(), Instant.EPOCH))
                .isEqualTo(new WooOrderCursor(Instant.parse("2026-09-21T10:00:00Z"), 2));
    }

    @Test
    void the_page_carries_the_original_payload_for_every_order() {
        stubOrders(pageOf("2026-09-21T10:10:00"));

        OrderPage page = driver.fetchOrders(context(), OrderQuery.since(Instant.parse("2026-09-21T10:00:00Z"), 2));

        RemoteOrder order = page.orders().getFirst();
        assertThat(order.rawPayload()).contains("\"date_modified_gmt\"");
        assertThat(order.order().channelOrderId()).isNotBlank();
    }

    @Test
    void a_429_penalises_the_rate_limiter_and_translates_to_rate_limit() {
        wireMock.stubFor(get(urlPathEqualTo("/wp-json/wc/v3/orders"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Retry-After", "30")
                        .withBody("{\"code\":\"woocommerce_rest_too_many_requests\",\"message\":\"slow down\"}")));

        assertThatThrownBy(
                        () -> driver.fetchOrders(context(), OrderQuery.since(Instant.parse("2026-09-21T10:00:00Z"), 2)))
                .isInstanceOfSatisfying(WooApiException.class, e -> {
                    DriverError error = driver.translate(e);
                    assertThat(error.errorClass()).isEqualTo(ErrorClass.RATE_LIMIT);
                });
        assertThat(rateLimiter.penalties).containsExactly(Duration.ofSeconds(30));
    }

    @Test
    void a_401_translates_to_auth_with_a_suggested_action() {
        wireMock.stubFor(get(urlPathEqualTo("/wp-json/wc/v3/orders"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"woocommerce_rest_cannot_view\",\"message\":\"denied\"}")));

        assertThatThrownBy(
                        () -> driver.fetchOrders(context(), OrderQuery.since(Instant.parse("2026-09-21T10:00:00Z"), 2)))
                .isInstanceOfSatisfying(WooApiException.class, e -> {
                    DriverError error = driver.translate(e);
                    assertThat(error.errorClass()).isEqualTo(ErrorClass.AUTH);
                    assertThat(error.suggestedAction()).isNotBlank();
                });
    }

    @Test
    void fetch_order_returns_empty_on_404() {
        wireMock.stubFor(get(urlPathEqualTo("/wp-json/wc/v3/orders/999"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"woocommerce_rest_shop_order_invalid_id\",\"message\":\"unknown\"}")));

        Optional<RemoteOrder> order = driver.fetchOrder(context(), "999");

        assertThat(order).isEmpty();
    }

    private void stubOrders(String body) {
        wireMock.stubFor(get(urlPathEqualTo("/wp-json/wc/v3/orders"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-WP-Total", "42")
                        .withBody(body)));
    }

    /** A page of minimal but realistic orders, one per modification timestamp given. */
    private static String pageOf(String... modifiedAt) {
        List<String> orders = new ArrayList<>();
        for (int i = 0; i < modifiedAt.length; i++) {
            orders.add("""
                    {"id": %d, "status": "processing", "currency": "EUR",
                     "date_created_gmt": "%s", "date_modified_gmt": "%s", "customer_id": 7,
                     "total": "10.00", "shipping_total": "0.00", "total_tax": "0.00",
                     "billing": {"first_name": "Mario", "last_name": "Rossi", "address_1": "Via Roma 1",
                                 "city": "Milano", "postcode": "20100", "country": "IT",
                                 "email": "mario@test.it"},
                     "shipping": {},
                     "line_items": [{"id": %d, "product_id": 1, "variation_id": 0, "quantity": 1,
                                     "sku": "SKU-%d", "total": "10.00"}]}""".formatted(2000 + i, modifiedAt[i], modifiedAt[i], 3000 + i, i));
        }
        return "[" + String.join(",", orders) + "]";
    }

    private ChannelContext context() {
        return new ChannelContext(
                TenantId.of("acme"),
                ChannelId.of("woo-local"),
                "https://localhost:" + wireMock.httpsPort(),
                Locale.ITALY,
                "EUR",
                ChannelCredentials.basic("ck_test", "cs_test"),
                Map.of(),
                rateLimiter);
    }

    private static WooCommerceApiClient trustingClient() {
        try {
            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(null, new TrustManager[] {new TrustEverything()}, new SecureRandom());
            return new WooCommerceApiClient(
                    HttpClient.newBuilder()
                            .sslContext(ssl)
                            .connectTimeout(Duration.ofSeconds(3))
                            .build(),
                    Duration.ofSeconds(10));
        } catch (Exception e) {
            throw new IllegalStateException("cannot build the test TLS context", e);
        }
    }

    /** WireMock's certificate is self-signed: trusting it is the point of this test, not a risk. */
    private static final class TrustEverything implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    /** Records what the driver asked of the limiter, so the 429 handling is observable. */
    private static final class RecordingRateLimiter implements RateLimiter {

        private final List<Duration> penalties = new ArrayList<>();

        @Override
        public boolean acquire(int permits, Duration timeout) {
            return true;
        }

        @Override
        public void penalize(Duration retryAfter) {
            penalties.add(retryAfter);
        }
    }
}
