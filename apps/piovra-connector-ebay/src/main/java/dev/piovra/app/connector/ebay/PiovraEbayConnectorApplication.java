package dev.piovra.app.connector.ebay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * eBay connector deployable: driver host, command consumer, order polling.
 *
 * <p>This module holds no logic: it assembles service modules into a deployable. Splitting a
 * service out later means creating a new module like this one and dropping the dependency from
 * here - not a single line of logic changes (docs/10-stack-and-repo.md).
 *
 * <p>Scanning starts at {@code dev.piovra} because components, entities and repositories live in
 * the service modules, not here.
 */
@SpringBootApplication(scanBasePackages = "dev.piovra")
@EntityScan("dev.piovra")
@EnableJpaRepositories("dev.piovra")
public class PiovraEbayConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PiovraEbayConnectorApplication.class, args);
    }
}
