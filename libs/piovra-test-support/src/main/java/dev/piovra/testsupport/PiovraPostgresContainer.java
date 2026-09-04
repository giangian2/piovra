package dev.piovra.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A single Postgres container shared by every test in the JVM (the "singleton container" pattern):
 * started once in a static initializer and never stopped explicitly - Testcontainers' Ryuk reaper
 * cleans it up when the JVM exits. Starting one container per test class would multiply a several
 * seconds startup cost across every module's test suite for no benefit.
 */
public final class PiovraPostgresContainer {

    private static final PostgreSQLContainer INSTANCE = new PostgreSQLContainer(
                    DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("piovra")
            .withUsername("piovra")
            .withPassword("piovra");

    static {
        INSTANCE.start();
    }

    private PiovraPostgresContainer() {}

    public static PostgreSQLContainer instance() {
        return INSTANCE;
    }

    public static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", INSTANCE::getUsername);
        registry.add("spring.datasource.password", INSTANCE::getPassword);
    }
}
