package dev.piovra.app.core;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One Flyway per module schema, rather than Spring Boot's single autoconfigured instance.
 *
 * <p>Each module numbers its own migrations from V1 and owns its own schema
 * (docs/12-development-guidelines.md section 5.5). A single Flyway pointed at all five locations
 * sees five files called V1 and refuses to start ("Found more than one migration with version 1"):
 * version numbers are only unique within a module, which is exactly what keeps the modules
 * independent and the future split into separate deployables free.
 *
 * <p>Each instance gets its own {@code flyway_schema_history} inside its own schema, so a module
 * splitting out later takes its migration history with it. Boot detects these beans as database
 * initializers and holds the entity manager factory back until they have all run.
 */
@Configuration(proxyBeanMethods = false)
class PiovraCoreFlywayConfiguration {

    @Bean
    FlywayMigrationInitializer catalogFlywayInitializer(DataSource dataSource) {
        return migrationsFor(dataSource, "catalog");
    }

    @Bean
    FlywayMigrationInitializer inventoryFlywayInitializer(DataSource dataSource) {
        return migrationsFor(dataSource, "inventory");
    }

    @Bean
    FlywayMigrationInitializer orderFlywayInitializer(DataSource dataSource) {
        return migrationsFor(dataSource, "orders");
    }

    @Bean
    FlywayMigrationInitializer publicationFlywayInitializer(DataSource dataSource) {
        return migrationsFor(dataSource, "publication");
    }

    @Bean
    FlywayMigrationInitializer channelConfigFlywayInitializer(DataSource dataSource) {
        return migrationsFor(dataSource, "channel_config");
    }

    /** The schema name is also the migration folder: one name, one place, nothing to keep in sync. */
    private static FlywayMigrationInitializer migrationsFor(DataSource dataSource, String schema) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration/" + schema)
                .load();
        return new FlywayMigrationInitializer(flyway);
    }
}
