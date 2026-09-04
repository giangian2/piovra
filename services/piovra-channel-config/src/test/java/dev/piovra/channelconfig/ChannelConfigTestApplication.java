package dev.piovra.channelconfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Minimal bootstrap so this module's integration tests can boot a real Spring context, mirroring
 * how apps/piovra-core assembles service modules. */
@SpringBootApplication(scanBasePackages = "dev.piovra")
@EntityScan("dev.piovra")
@EnableJpaRepositories("dev.piovra")
public class ChannelConfigTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChannelConfigTestApplication.class, args);
    }
}
