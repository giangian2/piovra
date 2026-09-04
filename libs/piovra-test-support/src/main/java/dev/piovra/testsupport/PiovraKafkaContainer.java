package dev.piovra.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/** Singleton Kafka container shared by every test in the JVM (see {@link PiovraPostgresContainer}). */
public final class PiovraKafkaContainer {

    private static final KafkaContainer INSTANCE = new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.0"));

    static {
        INSTANCE.start();
    }

    private PiovraKafkaContainer() {}

    public static KafkaContainer instance() {
        return INSTANCE;
    }

    public static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", INSTANCE::getBootstrapServers);
    }
}
