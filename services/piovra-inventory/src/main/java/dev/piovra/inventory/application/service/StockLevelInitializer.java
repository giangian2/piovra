package dev.piovra.inventory.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.piovra.events.ProductChanged;
import dev.piovra.inventory.application.port.out.StockLevelRepository;

/**
 * Guarantees a {@code stock_level} row exists (at zero) for every SKU the catalog knows about, so a
 * lookup from publication never has to distinguish "zero stock" from "SKU never seen". Naturally
 * idempotent ({@code ensureExists} is insert-if-absent), no {@code @Idempotent} needed - same
 * reasoning as {@code ChannelConfigCacheUpdater}.
 */
@Service
public class StockLevelInitializer {

    private final StockLevelRepository repository;

    public StockLevelInitializer(StockLevelRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void handle(ProductChanged event) {
        event.variantSkus().forEach(sku -> repository.ensureExists(event.tenantId(), sku));
    }
}
