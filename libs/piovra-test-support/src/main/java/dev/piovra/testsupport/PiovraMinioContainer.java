package dev.piovra.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A single MinIO container shared by every test in the JVM, like {@link PiovraPostgresContainer}.
 *
 * <p>Unlike Postgres and Kafka this one is <b>not</b> registered by {@link PiovraIntegrationTest}:
 * only the components that archive payloads need object storage, and starting a container for every
 * integration test in every module would be paid by all of them for the benefit of two.
 *
 * <p>The bucket is not created here - that would drag the AWS SDK into this module. A test creates
 * its own, which is one line through the client it is testing anyway.
 */
public final class PiovraMinioContainer {

    /** The S3 API port; 9001 is the web console, which no test needs. */
    private static final int API_PORT = 9000;

    /** quay.io is MinIO's own registry. Docker Hub's minio/minio is not pullable everywhere. */
    private static final String IMAGE = "quay.io/minio/minio:latest";

    private static final MinIOContainer INSTANCE = new MinIOContainer(
                    DockerImageName.parse(IMAGE).asCompatibleSubstituteFor("minio/minio"))
            .withUserName("piovra")
            .withPassword("piovra123");

    static {
        INSTANCE.start();
    }

    private PiovraMinioContainer() {}

    public static MinIOContainer instance() {
        return INSTANCE;
    }

    public static String endpoint() {
        return "http://" + INSTANCE.getHost() + ":" + INSTANCE.getMappedPort(API_PORT);
    }

    /** @param prefix the configuration prefix of the component under test, e.g. {@code piovra.connector.storage} */
    public static void registerProperties(DynamicPropertyRegistry registry, String prefix, String bucket) {
        registry.add(prefix + ".endpoint", PiovraMinioContainer::endpoint);
        registry.add(prefix + ".bucket", () -> bucket);
        registry.add(prefix + ".region", () -> "us-east-1");
        registry.add(prefix + ".access-key", INSTANCE::getUserName);
        registry.add(prefix + ".secret-key", INSTANCE::getPassword);
    }
}
