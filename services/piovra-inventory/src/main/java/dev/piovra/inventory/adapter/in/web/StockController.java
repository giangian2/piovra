package dev.piovra.inventory.adapter.in.web;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.piovra.common.TenantId;
import dev.piovra.events.InventoryChanged;
import dev.piovra.inventory.application.port.in.ApplyStockMovementsUseCase;

@RestController
@RequestMapping("/v1/stock")
public class StockController {

    private final ApplyStockMovementsUseCase applyStockMovementsUseCase;

    public StockController(ApplyStockMovementsUseCase applyStockMovementsUseCase) {
        this.applyStockMovementsUseCase = applyStockMovementsUseCase;
    }

    /** Batch SET: sets the available quantity of every listed SKU. Resubmitting the same
     * {@code batchId} is a no-op (see {@link StockSetBatchRequest}). */
    @PostMapping
    public List<InventoryChanged> applyBatch(
            @RequestHeader(value = "X-Piovra-Tenant", defaultValue = "default") String tenantId,
            @Valid @RequestBody StockSetBatchRequest request) {
        return applyStockMovementsUseCase.apply(TenantId.of(tenantId), request.toCommands(TenantId.of(tenantId)));
    }
}
