package dev.piovra.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "dev.piovra")
@EntityScan("dev.piovra")
@EnableJpaRepositories("dev.piovra")
public class CatalogTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogTestApplication.class, args);
    }
}
