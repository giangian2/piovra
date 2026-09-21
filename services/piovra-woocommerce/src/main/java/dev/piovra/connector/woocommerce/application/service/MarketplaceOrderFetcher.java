package dev.piovra.connector.woocommerce.application.service;

import org.springframework.stereotype.Component;

import dev.piovra.crosscutting.annotation.ChannelCall;
import dev.piovra.driver.spi.ChannelContext;
import dev.piovra.driver.spi.MarketplaceDriver;
import dev.piovra.driver.spi.OrderPage;
import dev.piovra.driver.spi.OrderQuery;

/**
 * A one-line bean whose only job is to be a different object from its caller.
 *
 * <p>{@code @ChannelCall} is an aspect, and Spring AOP works through proxies: calling the annotated
 * method from inside {@code OrderPollingService} would bypass the proxy and silently produce no
 * metric at all (docs/12-development-guidelines.md section 3.3, "beware of self-invocation").
 */
@Component
public class MarketplaceOrderFetcher {

    private final MarketplaceDriver driver;

    public MarketplaceOrderFetcher(MarketplaceDriver driver) {
        this.driver = driver;
    }

    @ChannelCall(operation = "ORDER_FETCH")
    public OrderPage fetch(ChannelContext context, OrderQuery query) {
        return driver.fetchOrders(context, query);
    }
}
