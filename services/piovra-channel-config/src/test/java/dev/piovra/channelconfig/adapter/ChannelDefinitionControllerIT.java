package dev.piovra.channelconfig.adapter;

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

import dev.piovra.channelconfig.adapter.in.web.ChannelDefinitionRequest;
import dev.piovra.common.Ids;
import dev.piovra.model.channel.ChannelDefinition;
import dev.piovra.testsupport.ChannelDefinitionFixtures;
import dev.piovra.testsupport.PiovraIntegrationTest;

/** Exercises the real wire format end to end, mirroring catalog's {@code ProductControllerIT}. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChannelDefinitionControllerIT extends PiovraIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void registering_a_channel_returns_200_and_it_can_be_read_back() throws Exception {
        String channelId = "http-" + Ids.newId().toLowerCase();

        HttpResponse<String> put = send("PUT", "/v1/channels/" + channelId, requestBodyFor(channelId));
        assertThat(put.statusCode()).isEqualTo(200);

        HttpResponse<String> get = send("GET", "/v1/channels/" + channelId, null);
        assertThat(get.statusCode()).isEqualTo(200);
        assertThat(get.body()).contains(channelId);
    }

    @Test
    void getting_an_unknown_channel_returns_404() throws Exception {
        HttpResponse<String> response =
                send("GET", "/v1/channels/unknown-" + Ids.newId().toLowerCase(), null);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void listing_channels_includes_a_freshly_registered_one() throws Exception {
        String channelId = "http-" + Ids.newId().toLowerCase();
        send("PUT", "/v1/channels/" + channelId, requestBodyFor(channelId));

        HttpResponse<String> list = send("GET", "/v1/channels", null);

        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.body()).contains(channelId);
    }

    private String requestBodyFor(String channelId) throws Exception {
        ChannelDefinition fixture = ChannelDefinitionFixtures.channel(channelId);
        ChannelDefinitionRequest request = new ChannelDefinitionRequest(
                fixture.type(),
                fixture.marketplaceCode(),
                fixture.enabled(),
                fixture.credentialsRef(),
                fixture.policy(),
                fixture.categoryMapping(),
                fixture.settings());
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
