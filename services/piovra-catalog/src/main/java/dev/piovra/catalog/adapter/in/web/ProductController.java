package dev.piovra.catalog.adapter.in.web;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.piovra.catalog.application.port.in.FindProductUseCase;
import dev.piovra.catalog.application.port.in.UpsertProductUseCase;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.events.ProductChanged;
import dev.piovra.model.product.CanonicalProduct;

@RestController
@RequestMapping("/v1/products")
public class ProductController {

    private final UpsertProductUseCase upsertProductUseCase;
    private final FindProductUseCase findProductUseCase;

    public ProductController(UpsertProductUseCase upsertProductUseCase, FindProductUseCase findProductUseCase) {
        this.upsertProductUseCase = upsertProductUseCase;
        this.findProductUseCase = findProductUseCase;
    }

    /** 200 with the persisted product when something changed, 204 when the payload was a no-op. */
    @PutMapping("/{sku}")
    public ResponseEntity<CanonicalProduct> upsert(
            @PathVariable String sku,
            @RequestHeader(value = "X-Piovra-Tenant", defaultValue = "default") String tenantId,
            @RequestBody ProductRequest request) {
        TenantId tenant = TenantId.of(tenantId);
        Optional<ProductChanged> changed = upsertProductUseCase.upsert(tenant, request.toProduct(tenant, Sku.of(sku)));
        return changed.map(event -> ResponseEntity.ok(event.product()))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{sku}")
    public CanonicalProduct get(
            @PathVariable String sku,
            @RequestHeader(value = "X-Piovra-Tenant", defaultValue = "default") String tenantId) {
        return findProductUseCase
                .find(TenantId.of(tenantId), Sku.of(sku))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "product not found: " + sku));
    }
}
