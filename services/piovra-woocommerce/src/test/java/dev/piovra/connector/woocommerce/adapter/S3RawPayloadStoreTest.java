package dev.piovra.connector.woocommerce.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.TenantId;
import dev.piovra.connector.woocommerce.application.port.out.RawPayloadStore;
import dev.piovra.testsupport.PiovraIntegrationTest;
import dev.piovra.testsupport.PiovraMinioContainer;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/** Against a real MinIO: object storage is the one genuinely new external dependency of this slice. */
class S3RawPayloadStoreTest extends PiovraIntegrationTest {

    private static final String BUCKET = "piovra-raw-payloads";

    @Autowired
    private RawPayloadStore store;

    @Autowired
    private S3Client s3;

    @DynamicPropertySource
    static void registerStorage(DynamicPropertyRegistry registry) {
        PiovraMinioContainer.registerProperties(registry, "piovra.connector.storage", BUCKET);
    }

    @BeforeEach
    void createBucket() {
        try {
            s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        } catch (BucketAlreadyOwnedByYouException alreadyThere) {
            // The container is shared by the whole JVM, so the first test already made it.
        }
    }

    @Test
    void a_payload_is_stored_and_the_returned_uri_points_at_it() {
        String channelOrderId = Ids.newId();
        String payload = "{\"id\":" + 1 + ",\"buyer\":\"Mario Rossi\"}";

        String uri = store.store(TenantId.DEFAULT, ChannelId.of("woo-local"), channelOrderId, payload);

        assertThat(uri).startsWith("s3://" + BUCKET + "/").endsWith(channelOrderId + ".json");
        assertThat(read(keyOf(uri))).isEqualTo(payload);
    }

    @Test
    void the_key_is_partitioned_by_tenant_and_channel() {
        String uri = store.store(TenantId.of("acme"), ChannelId.of("woo-it"), Ids.newId(), "{}");

        assertThat(keyOf(uri)).startsWith("acme/woo-it/");
    }

    @Test
    void storing_the_same_order_twice_overwrites_rather_than_duplicating() {
        String channelOrderId = Ids.newId();
        store.store(TenantId.DEFAULT, ChannelId.of("woo-local"), channelOrderId, "{\"v\":1}");

        String uri = store.store(TenantId.DEFAULT, ChannelId.of("woo-local"), channelOrderId, "{\"v\":2}");

        // Re-reading a page after a crash must not leave two copies of the same order behind.
        assertThat(read(keyOf(uri))).isEqualTo("{\"v\":2}");
    }

    private String keyOf(String uri) {
        return uri.substring(("s3://" + BUCKET + "/").length());
    }

    private String read(String key) {
        ResponseBytes<?> bytes = s3.getObjectAsBytes(
                GetObjectRequest.builder().bucket(BUCKET).key(key).build());
        return new String(bytes.asByteArray(), StandardCharsets.UTF_8);
    }
}
