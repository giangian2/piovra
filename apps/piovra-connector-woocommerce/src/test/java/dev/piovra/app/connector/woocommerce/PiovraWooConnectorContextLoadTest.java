package dev.piovra.app.connector.woocommerce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import dev.piovra.connector.woocommerce.adapter.in.scheduling.OrderPollScheduler;
import dev.piovra.outbox.OutboxWriter;
import dev.piovra.testsupport.PiovraIntegrationTest;

/**
 * Boots the real deployable against its own {@code application.yml}. This is what catches a drift
 * between the production configuration and the code - a Flyway location that does not exist, a
 * property with no binding class, a bean that cannot be built - before a deploy does.
 */
class PiovraWooConnectorContextLoadTest extends PiovraIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void the_connector_context_loads() {
        assertThat(context).isNotNull();
    }

    @Test
    void the_outbox_writer_resolves_as_a_single_named_bean() {
        Map<String, OutboxWriter> writers = context.getBeansOfType(OutboxWriter.class);

        assertThat(writers).containsOnlyKeys("wooOutboxWriter");
    }

    @Test
    void the_poll_scheduler_is_wired() {
        assertThat(context.getBeansOfType(OrderPollScheduler.class)).hasSize(1);
    }
}
