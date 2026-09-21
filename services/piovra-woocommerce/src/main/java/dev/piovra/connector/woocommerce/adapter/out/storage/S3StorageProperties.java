package dev.piovra.connector.woocommerce.adapter.out.storage;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** S3-compatible object storage for the raw marketplace payloads. Same shape works against MinIO
 * locally (endpointOverride + path-style) and real AWS S3 in production. */
@ConfigurationProperties(prefix = "piovra.connector.storage")
public record S3StorageProperties(URI endpoint, String bucket, String region, String accessKey, String secretKey) {}
