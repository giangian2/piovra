package dev.piovra.connector.woocommerce.adapter.out.storage;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import dev.piovra.common.ChannelId;
import dev.piovra.common.ErrorClass;
import dev.piovra.common.PiovraException;
import dev.piovra.common.TenantId;
import dev.piovra.connector.woocommerce.application.port.out.RawPayloadStore;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3RawPayloadStore implements RawPayloadStore {

    /** Date-partitioned like the feed archive (docs/02-services.md), so a day's traffic is one prefix. */
    private static final DateTimeFormatter DATE_PREFIX = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final S3Client s3;
    private final String bucket;

    public S3RawPayloadStore(S3Client connectorS3Client, S3StorageProperties properties) {
        this.s3 = connectorS3Client;
        this.bucket = properties.bucket();
    }

    @Override
    public String store(TenantId tenantId, ChannelId channelId, String channelOrderId, String payload) {
        String key = "%s/%s/%s/%s.json"
                .formatted(
                        tenantId.value(),
                        channelId.value(),
                        DATE_PREFIX.format(Instant.now().atOffset(ZoneOffset.UTC)),
                        channelOrderId);
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType("application/json")
                            .build(),
                    RequestBody.fromString(payload, StandardCharsets.UTF_8));
        } catch (NoSuchBucketException e) {
            throw new PiovraException(
                    ErrorClass.INTERNAL,
                    "RAW_PAYLOAD_BUCKET_MISSING",
                    "bucket " + bucket + " does not exist: create it before starting the connector",
                    e);
        }
        return "s3://" + bucket + "/" + key;
    }
}
