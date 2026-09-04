package dev.piovra.catalog.application.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.piovra.catalog.application.port.in.FindProductUseCase;
import dev.piovra.catalog.application.port.in.UpsertProductUseCase;
import dev.piovra.catalog.application.port.out.ProductRepository;
import dev.piovra.catalog.domain.service.CatalogUpsertService;
import dev.piovra.catalog.domain.service.UpsertPlan;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.events.ProductChanged;
import dev.piovra.model.product.CanonicalProduct;
import dev.piovra.outbox.OutboxWriter;

@Service
public class ProductUpsertService implements UpsertProductUseCase, FindProductUseCase {

    private final ProductRepository repository;
    private final OutboxWriter outboxWriter;

    public ProductUpsertService(ProductRepository repository, OutboxWriter outboxWriter) {
        this.repository = repository;
        this.outboxWriter = outboxWriter;
    }

    @Override
    @Transactional
    public Optional<ProductChanged> upsert(TenantId tenantId, CanonicalProduct draft) {
        Optional<CanonicalProduct> existing = repository.findBySku(tenantId, draft.sku());
        UpsertPlan plan = CatalogUpsertService.plan(existing, draft);

        if (plan.noop()) {
            return Optional.empty();
        }

        CanonicalProduct saved = repository.save(plan.product());
        ProductChanged event =
                switch (plan.changeType()) {
                    case CREATED -> ProductChanged.created(saved);
                    case UPDATED -> ProductChanged.updated(saved, plan.changedFields());
                    case DISCONTINUED -> ProductChanged.discontinued(saved, plan.changedFields());
                };
        outboxWriter.append(event);
        return Optional.of(event);
    }

    @Override
    public Optional<CanonicalProduct> find(TenantId tenantId, Sku sku) {
        return repository.findBySku(tenantId, sku);
    }
}
