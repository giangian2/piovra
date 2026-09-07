package dev.piovra.publication.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.publication.application.port.out.ChannelListingRepository;
import dev.piovra.publication.domain.ChannelListing;
import dev.piovra.testsupport.PiovraIntegrationTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChannelListingControllerIT extends PiovraIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ChannelListingRepository channelListingRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void a_known_listing_is_readable_by_sku_and_channel() throws Exception {
        Sku sku = Sku.of("TEST-" + Ids.newId());
        ChannelId channel = ChannelId.of("test-channel");
        channelListingRepository.save(
                ChannelListing.notListed(TenantId.DEFAULT, sku, channel).markBlocked("category not mapped", null));

        HttpResponse<String> bySku = send("/v1/listings/" + sku.value());
        assertThat(bySku.statusCode()).isEqualTo(200);
        assertThat(bySku.body()).contains("BLOCKED").contains(channel.value());

        HttpResponse<String> byChannel = send("/v1/listings/" + sku.value() + "/" + channel.value());
        assertThat(byChannel.statusCode()).isEqualTo(200);
        assertThat(byChannel.body()).contains("BLOCKED");
    }

    @Test
    void an_unknown_sku_returns_an_empty_list_not_a_404() throws Exception {
        HttpResponse<String> response = send("/v1/listings/UNKNOWN-" + Ids.newId());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("[]");
    }

    @Test
    void an_unknown_sku_and_channel_pair_returns_404() throws Exception {
        HttpResponse<String> response = send("/v1/listings/UNKNOWN-" + Ids.newId() + "/unknown-channel");

        assertThat(response.statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> send(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("X-Piovra-Tenant", "default")
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
