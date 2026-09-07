package dev.piovra.inventory.adapter.in.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import dev.piovra.common.Ids;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.events.InventoryChanged;
import dev.piovra.inventory.application.port.in.StockMovementCommand;
import dev.piovra.inventory.application.port.in.StockMovementMode;

/**
 * Request body of {@code POST /v1/stock}. {@code batchId} is required from the caller, never
 * server-generated: it is what lets a future FTP-fed bulk update (feed-processor supplying its
 * {@code feedId} as {@code batchId}) reuse this exact endpoint/method with no rework - resubmitting
 * the same batch is a no-op, not a duplicate application of every line.
 */
public record StockSetBatchRequest(
        @NotBlank String batchId, @NotEmpty List<@Valid StockLine> lines) {

    public List<StockMovementCommand> toCommands(TenantId tenantId) {
        return lines.stream()
                .map(line -> {
                    Sku sku = Sku.of(line.sku());
                    return new StockMovementCommand(
                            sku,
                            StockMovementMode.SET,
                            line.quantity(),
                            InventoryChanged.Reason.FEED_SET,
                            null,
                            Ids.stockSetKey(batchId, sku));
                })
                .toList();
    }
}
