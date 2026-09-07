package dev.piovra.order.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.common.Ids;
import dev.piovra.common.Money;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.model.order.Address;
import dev.piovra.model.order.Buyer;
import dev.piovra.model.order.OrderStatus;
import dev.piovra.model.order.OrderTotals;
import dev.piovra.order.adapter.in.web.OrderIngestRequest;
import dev.piovra.order.adapter.in.web.OrderLineRequest;
import dev.piovra.order.application.port.out.KnownSkuRepository;
import dev.piovra.testsupport.PiovraIntegrationTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderControllerIT extends PiovraIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KnownSkuRepository knownSkuRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void a_known_sku_is_accepted_and_readable_back() throws Exception {
        Sku sku = Sku.of("TEST-" + Ids.newId());
        knownSkuRepository.ensureExists(TenantId.DEFAULT, sku);

        HttpResponse<String> post = send("POST", "/v1/orders", requestBody(sku));
        assertThat(post.statusCode()).isEqualTo(202);

        JsonNode body = objectMapper.readTree(post.body());
        assertThat(body.get("lines").get(0).get("resolution").asText()).isEqualTo("MAPPED");
        String orderId = body.get("orderId").asText();

        HttpResponse<String> get = send("GET", "/v1/orders/" + orderId, null);
        assertThat(get.statusCode()).isEqualTo(200);
    }

    @Test
    void an_unknown_sku_is_stored_unmapped() throws Exception {
        Sku unknownSku = Sku.of("UNKNOWN-" + Ids.newId());

        HttpResponse<String> post = send("POST", "/v1/orders", requestBody(unknownSku));

        assertThat(post.statusCode()).isEqualTo(202);
        JsonNode body = objectMapper.readTree(post.body());
        assertThat(body.get("lines").get(0).get("resolution").asText()).isEqualTo("UNMAPPED");
    }

    @Test
    void an_unknown_order_id_returns_404() throws Exception {
        HttpResponse<String> response = send("GET", "/v1/orders/UNKNOWN-" + Ids.newId(), null);

        assertThat(response.statusCode()).isEqualTo(404);
    }

    private String requestBody(Sku sku) throws Exception {
        OrderIngestRequest request = new OrderIngestRequest(
                "test-channel",
                "CH-" + Ids.newId(),
                OrderStatus.NEW,
                "processing",
                Instant.now(),
                new Buyer("buyer-1", "Mario Rossi", "mario@test.it"),
                new Address("Mario Rossi", "Via Roma 1", null, "Milano", "MI", "20100", "IT", null),
                new OrderTotals(Money.euro("19.90"), Money.euro("0.00"), Money.euro("0.00"), Money.euro("19.90")),
                List.of(new OrderLineRequest("line-1", "channel-line-1", sku.value(), 1, Money.euro("19.90"))));
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
