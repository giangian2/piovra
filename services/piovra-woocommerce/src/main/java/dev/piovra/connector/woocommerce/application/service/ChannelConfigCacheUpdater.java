package dev.piovra.connector.woocommerce.application.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.piovra.connector.woocommerce.application.port.out.ChannelDefinitionCache;
import dev.piovra.connector.woocommerce.application.port.out.PollCursorStore;
import dev.piovra.connector.woocommerce.application.port.out.PollKind;
import dev.piovra.connector.woocommerce.config.ConnectorProperties;
import dev.piovra.events.ChannelConfigChanged;
import dev.piovra.model.channel.ChannelDefinition;

/**
 * Keeps the local channel cache in sync and gives every new channel of our own type a polling
 * cursor.
 *
 * <p>Naturally idempotent - an upsert by key on a compacted topic - so it needs no
 * {@code @Idempotent}: replaying the same event is a no-op by construction
 * (docs/12-development-guidelines.md section 3.2).
 */
@Service
public class ChannelConfigCacheUpdater {

    private final ChannelDefinitionCache cache;
    private final PollCursorStore cursorStore;
    private final ConnectorProperties properties;

    public ChannelConfigCacheUpdater(
            ChannelDefinitionCache cache, PollCursorStore cursorStore, ConnectorProperties properties) {
        this.cache = cache;
        this.cursorStore = cursorStore;
        this.properties = properties;
    }

    @Transactional
    public void handle(ChannelConfigChanged event) {
        ChannelDefinition definition = event.definition();
        cache.upsert(definition);

        if (definition.type() != properties.channelType()) {
            // Every connector sees the whole compacted topic; only its own type concerns it.
            return;
        }
        // The cursor starts at "now", not at the beginning of time: connecting a store must not
        // replay years of its order history into the system. What came before Piovra is not ours.
        cursorStore.ensureExists(definition.tenantId(), definition.channelId(), PollKind.ORDERS, Instant.now());
    }
}
