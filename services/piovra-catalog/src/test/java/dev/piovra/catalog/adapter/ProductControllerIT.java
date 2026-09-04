package dev.piovra.catalog.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.catalog.adapter.in.web.ProductRequest;
import dev.piovra.common.Ids;
import dev.piovra.model.product.CanonicalProduct;
import dev.piovra.testsupport.CanonicalProductFixtures;
import dev.piovra.testsupport.PiovraIntegrationTest;

/**
 * Exercises the real wire format end to end: the same {@link ObjectMapper} bean the controller uses
 * serializes the request, and an embedded server receives it over real HTTP - the one layer none of
 * the other catalog tests (persistence, outbox) actually touch.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductControllerIT extends PiovraIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void putting_a_new_product_returns_200_and_it_can_be_read_back() throws Exception {
        String sku = "HTTP-" + Ids.newId();

        HttpResponse<String> put = send("PUT", "/v1/products/" + sku, requestBodyFor(sku));
        assertThat(put.statusCode()).isEqualTo(200);

        HttpResponse<String> get = send("GET", "/v1/products/" + sku, null);
        assertThat(get.statusCode()).isEqualTo(200);
        assertThat(get.body()).contains(sku);
    }

    @Test
    void resubmitting_the_identical_product_returns_204() throws Exception {
        String sku = "HTTP-" + Ids.newId();
        String body = requestBodyFor(sku);

        send("PUT", "/v1/products/" + sku, body);
        HttpResponse<String> second = send("PUT", "/v1/products/" + sku, body);

        assertThat(second.statusCode()).isEqualTo(204);
    }

    @Test
    void getting_an_unknown_sku_returns_404() throws Exception {
        HttpResponse<String> response = send("GET", "/v1/products/UNKNOWN-" + Ids.newId(), null);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    private String requestBodyFor(String sku) throws Exception {
        CanonicalProduct fixture = CanonicalProductFixtures.simpleProduct(sku);
        ProductRequest request = new ProductRequest(
                fixture.status(),
                fixture.type(),
                fixture.title(),
                fixture.description(),
                fixture.brand(),
                fixture.categoryPath(),
                fixture.identifiers(),
                fixture.media(),
                fixture.attributes(),
                fixture.variantAxes(),
                fixture.variants(),
                fixture.channelOverrides());
        return objectMapper.writeValueAsString(request);
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .header("X-Piovra-Tenant", "default")
                .method(
                        method,
                        body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
