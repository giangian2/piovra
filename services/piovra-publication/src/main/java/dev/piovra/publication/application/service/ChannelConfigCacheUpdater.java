package dev.piovra.publication.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.piovra.events.ChannelConfigChanged;
import dev.piovra.publication.application.port.out.ChannelDefinitionCache;

/**
 * Keeps the local channel cache in sync. Naturally idempotent (upsert by key, on a compacted topic),
 * so it does not need {@code @Idempotent}: the mechanism would add nothing here
 * (docs/12-development-guidelines.md section 3.2 - "the logic must be identical at every call site,
 * and genuinely cross-cutting").
 */
@Service
public class ChannelConfigCacheUpdater {

    private final ChannelDefinitionCache cache;

    public ChannelConfigCacheUpdater(ChannelDefinitionCache cache) {
        this.cache = cache;
    }

    @Transactional
    public void handle(ChannelConfigChanged event) {
        cache.upsert(event.definition());
    }
}
