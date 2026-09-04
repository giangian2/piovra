package dev.piovra.testsupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for a service's Postgres+Kafka integration tests: repositories, outbox relays,
 * consumers (docs/12-development-guidelines.md section 6, "Integration" row).
 */
@SpringBootTest
public abstract class PiovraIntegrationTest {

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        PiovraPostgresContainer.registerProperties(registry);
        PiovraKafkaContainer.registerProperties(registry);
    }
}
