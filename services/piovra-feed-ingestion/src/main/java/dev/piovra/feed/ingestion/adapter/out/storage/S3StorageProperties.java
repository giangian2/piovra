package dev.piovra.feed.ingestion.adapter.out.storage;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** S3-compatible object storage for the immutable raw feed archive (docs/05-feed-flow.md). Same
 * shape works against MinIO locally (endpointOverride + path-style) and real AWS S3 in production. */
@ConfigurationProperties(prefix = "piovra.feed.storage")
public record S3StorageProperties(URI endpoint, String bucket, String region, String accessKey, String secretKey) {}
