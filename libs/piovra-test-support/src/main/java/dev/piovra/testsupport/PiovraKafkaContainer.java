package dev.piovra.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/** Singleton Kafka container shared by every test in the JVM (see {@link PiovraPostgresContainer}). */
public final class PiovraKafkaContainer {

    private static final KafkaContainer INSTANCE = new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.0"))
            // Testcontainers binds every listener to 0.0.0.0, the controller one included. Since 3.9
            // Kafka derives the ADVERTISED controller address from `listeners` - there is no advertised
            // entry for CONTROLLER - and then rejects 0.0.0.0 as non-routable, so the broker exits
            // before it ever starts ("advertised.listeners cannot use the nonroutable meta-address").
            // Leaving the controller host empty makes Kafka advertise the container's own hostname.
            // Only this entry changes: the other two keep Testcontainers' own values.
            .withEnv("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:9092,BROKER://0.0.0.0:9093,CONTROLLER://:9094");

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
