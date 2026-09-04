package dev.piovra.catalog.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import dev.piovra.catalog.application.port.out.ProductRepository;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.model.product.CanonicalProduct;

@Repository
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpaRepository;
    private final ProductEntityMapper mapper;

    public ProductRepositoryAdapter(ProductJpaRepository jpaRepository, ProductEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<CanonicalProduct> findBySku(TenantId tenantId, Sku sku) {
        return jpaRepository.findByTenantIdAndSku(tenantId.value(), sku.value()).map(mapper::toDomain);
    }

    @Override
    public CanonicalProduct save(CanonicalProduct product) {
        ProductEntity entity = jpaRepository
                .findByTenantIdAndSku(product.tenantId().value(), product.sku().value())
                .map(existing -> {
                    mapper.applyTo(existing, product);
                    return existing;
                })
                .orElseGet(() -> mapper.toNewEntity(product));
        jpaRepository.save(entity);
        return product;
    }
}
