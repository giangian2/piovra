package dev.piovra.order.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.piovra.events.ProductChanged;
import dev.piovra.order.application.port.out.KnownSkuRepository;

/** Keeps the local known-SKU cache in sync, so {@link OrderIngestionService} can tell "a real SKU"
 * from "unknown" without calling catalog-service directly. Naturally idempotent (insert-if-absent),
 * no {@code @Idempotent} needed - same reasoning as {@code StockLevelInitializer}. */
@Service
public class KnownSkuInitializer {

    private final KnownSkuRepository repository;

    public KnownSkuInitializer(KnownSkuRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void handle(ProductChanged event) {
        event.variantSkus().forEach(sku -> repository.ensureExists(event.tenantId(), sku));
    }
}
