package dev.piovra.connector.woocommerce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import dev.piovra.outbox.OutboxWriter;
import dev.piovra.testsupport.PiovraIntegrationTest;

/**
 * Boots the connector against a real Postgres and a real Kafka. This is the test that catches a
 * drift between the Flyway migrations and the JPA entities - {@code ddl-auto: validate} refuses to
 * start the context - before a deploy does.
 */
class WooConnectorContextLoadTest extends PiovraIntegrationTest {

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
}
