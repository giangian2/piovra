package dev.piovra.catalog.adapter.out.persistence;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.common.ErrorClass;
import dev.piovra.common.PiovraException;
import dev.piovra.model.product.CanonicalProduct;

@Component
public class ProductEntityMapper {

    private final ObjectMapper objectMapper;

    public ProductEntityMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ProductEntity toNewEntity(CanonicalProduct product) {
        return new ProductEntity(
                UUID.randomUUID(),
                product.tenantId().value(),
                product.sku().value(),
                product.revision(),
                product.status().name(),
                write(product));
    }

    public void applyTo(ProductEntity entity, CanonicalProduct product) {
        entity.update(product.revision(), product.status().name(), write(product));
    }

    public CanonicalProduct toDomain(ProductEntity entity) {
        return read(entity.payload());
    }

    private String write(CanonicalProduct product) {
        try {
            return objectMapper.writeValueAsString(product);
        } catch (JsonProcessingException e) {
            throw new PiovraException(ErrorClass.INTERNAL, "PRODUCT_SERIALIZATION", "cannot serialize product", e);
        }
    }

    private CanonicalProduct read(String payload) {
        try {
            return objectMapper.readValue(payload, CanonicalProduct.class);
        } catch (JsonProcessingException e) {
            throw new PiovraException(ErrorClass.INTERNAL, "PRODUCT_DESERIALIZATION", "cannot deserialize product", e);
        }
    }
}
