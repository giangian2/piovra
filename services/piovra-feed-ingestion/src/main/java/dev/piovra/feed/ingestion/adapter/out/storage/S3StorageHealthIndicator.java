package dev.piovra.feed.ingestion.adapter.out.storage;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

/** Surfaces object storage reachability on {@code /actuator/health} as {@code s3Storage}. This, plus
 * the {@link S3Client} bean, is the entire scope of S3 wiring for this step - no upload/download
 * logic yet (that belongs to feed-ingestion's business logic, out of scope here). */
@Component("s3Storage")
public class S3StorageHealthIndicator implements HealthIndicator {

    private final S3Client s3Client;
    private final S3StorageProperties properties;

    public S3StorageHealthIndicator(S3Client s3Client, S3StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public Health health() {
        try {
            s3Client.headBucket(
                    HeadBucketRequest.builder().bucket(properties.bucket()).build());
            return Health.up().withDetail("bucket", properties.bucket()).build();
        } catch (Exception e) {
            return Health.down(e).withDetail("bucket", properties.bucket()).build();
        }
    }
}
