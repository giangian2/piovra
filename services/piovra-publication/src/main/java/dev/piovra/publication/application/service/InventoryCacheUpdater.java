package dev.piovra.publication.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.piovra.events.InventoryChanged;
import dev.piovra.publication.application.port.out.InventoryCache;

/**
 * Keeps the local stock cache in sync. Naturally idempotent (upsert by key), no {@code @Idempotent}
 * needed - same reasoning as {@code ChannelConfigCacheUpdater}.
 */
@Service
public class InventoryCacheUpdater {

    private final InventoryCache cache;

    public InventoryCacheUpdater(InventoryCache cache) {
        this.cache = cache;
    }

    @Transactional
    public void handle(InventoryChanged event) {
        cache.upsert(event.tenantId(), event.sku(), event.available());
    }
}
