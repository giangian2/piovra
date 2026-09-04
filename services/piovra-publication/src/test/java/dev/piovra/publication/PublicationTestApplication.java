package dev.piovra.publication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "dev.piovra")
@EntityScan("dev.piovra")
@EnableJpaRepositories("dev.piovra")
public class PublicationTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(PublicationTestApplication.class, args);
    }
}
