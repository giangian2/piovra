package dev.piovra.app.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import dev.piovra.outbox.OutboxRelay;
import dev.piovra.outbox.OutboxWriter;
import dev.piovra.testsupport.PiovraIntegrationTest;

/**
 * Boots the real combined context - catalog, inventory, orders, publication, channel-config in one
 * {@link ApplicationContext}, exactly what {@code PiovraCoreApplication} assembles for production.
 *
 * <p>{@link ArchitectureTest} never catches wiring bugs: it is static classpath analysis, it never
 * refreshes a context. This is the test that would have failed on the ambiguous {@link OutboxWriter}
 * bean bug (three unqualified beans of the same type colliding once more than one outbox-writing
 * module shares a context) before it shipped.
 */
class PiovraCoreContextLoadIT extends PiovraIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void the_combined_context_loads() {
        assertThat(context).isNotNull();
    }

    @Test
    void every_module_s_outbox_writer_resolves_as_a_distinct_qualified_bean() {
        Map<String, OutboxWriter> writers = context.getBeansOfType(OutboxWriter.class);

        assertThat(writers)
                .containsOnlyKeys("catalogOutboxWriter", "channelConfigOutboxWriter", "publicationOutboxWriter");
        assertThat(writers.values()).doesNotHaveDuplicates();
    }

    @Test
    void every_module_s_outbox_relay_resolves_as_a_distinct_bean() {
        Map<String, OutboxRelay> relays = context.getBeansOfType(OutboxRelay.class);

        assertThat(relays)
                .containsOnlyKeys("catalogOutboxRelay", "channelConfigOutboxRelay", "publicationOutboxRelay");
    }
}
