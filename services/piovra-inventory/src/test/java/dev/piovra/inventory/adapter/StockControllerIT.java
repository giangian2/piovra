package dev.piovra.inventory.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.common.Ids;
import dev.piovra.inventory.adapter.in.web.StockLine;
import dev.piovra.inventory.adapter.in.web.StockSetBatchRequest;
import dev.piovra.testsupport.PiovraIntegrationTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StockControllerIT extends PiovraIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void a_valid_batch_sets_stock_and_returns_the_resulting_events() throws Exception {
        String sku = "TEST-" + Ids.newId();
        String body = objectMapper.writeValueAsString(
                new StockSetBatchRequest("batch-" + Ids.newId(), List.of(new StockLine(sku, 10))));

        HttpResponse<String> response = send(body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(sku).contains("\"available\":10");
    }

    @Test
    void resubmitting_the_same_batch_id_is_a_noop() throws Exception {
        String sku = "TEST-" + Ids.newId();
        String batchId = "batch-" + Ids.newId();
        String body =
                objectMapper.writeValueAsString(new StockSetBatchRequest(batchId, List.of(new StockLine(sku, 10))));

        send(body);
        HttpResponse<String> second = send(body);

        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(second.body()).isEqualTo("[]");
    }

    @Test
    void an_empty_batch_is_rejected() throws Exception {
        String body = objectMapper.writeValueAsString(new StockSetBatchRequest("batch-" + Ids.newId(), List.of()));

        HttpResponse<String> response = send(body);

        assertThat(response.statusCode()).isEqualTo(400);
    }

    private HttpResponse<String> send(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/v1/stock"))
                .header("Content-Type", "application/json")
                .header("X-Piovra-Tenant", "default")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
