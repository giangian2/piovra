package dev.piovra.connector.woocommerce.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.piovra.driver.spi.MarketplaceDriver;
import dev.piovra.driver.woocommerce.WooCommerceApiClient;
import dev.piovra.driver.woocommerce.WooCommerceDriver;

/** Turns the framework-free driver into a bean. The driver is stateless and handles every
 * WooCommerce account: per-account state lives here, keyed by channelId. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ConnectorProperties.class)
public class WooConnectorConfiguration {

    @Bean
    public MarketplaceDriver wooCommerceDriver() {
        return new WooCommerceDriver(WooCommerceApiClient.withDefaults());
    }
}
