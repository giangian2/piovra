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

import dev.piovra.catalog.adapter.in.web.ComplianceProfileRequest;
import dev.piovra.model.compliance.Address;
import dev.piovra.model.compliance.ComplianceProfileType;
import dev.piovra.testsupport.PiovraIntegrationTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ComplianceProfileControllerIT extends PiovraIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void registering_a_profile_returns_201_and_it_can_be_read_back() throws Exception {
        HttpResponse<String> post = send("POST", "/v1/compliance-profiles", requestBody());
        assertThat(post.statusCode()).isEqualTo(201);

        String id = objectMapper.readTree(post.body()).get("id").asText();
        HttpResponse<String> get = send("GET", "/v1/compliance-profiles/" + id, null);
        assertThat(get.statusCode()).isEqualTo(200);
        assertThat(get.body()).contains("Acme Srl");
    }

    @Test
    void getting_an_unknown_id_returns_404() throws Exception {
        HttpResponse<String> response = send("GET", "/v1/compliance-profiles/UNKNOWN-ID", null);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    private String requestBody() throws Exception {
        ComplianceProfileRequest request = new ComplianceProfileRequest(
                ComplianceProfileType.MANUFACTURER,
                "Acme Srl",
                new Address("Via Roma 1", "Milano", "20100", "IT"),
                "compliance@acme.test",
                null);
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
