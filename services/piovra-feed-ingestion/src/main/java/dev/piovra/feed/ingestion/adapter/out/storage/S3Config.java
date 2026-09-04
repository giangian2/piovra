package dev.piovra.feed.ingestion.adapter.out.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(S3StorageProperties.class)
public class S3Config {

    @Bean
    public S3Client s3Client(S3StorageProperties properties) {
        return S3Client.builder()
                .endpointOverride(properties.endpoint())
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
                // Required for MinIO: bucket-in-path (http://host:9000/bucket/key), not the
                // virtual-hosted subdomain style real S3 also supports.
                .forcePathStyle(true)
                .build();
    }
}
