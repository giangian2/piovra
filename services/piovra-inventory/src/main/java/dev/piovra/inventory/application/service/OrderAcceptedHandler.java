package dev.piovra.inventory.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.piovra.common.Ids;
import dev.piovra.events.InventoryChanged;
import dev.piovra.events.OrderAccepted;
import dev.piovra.inventory.application.port.in.ApplyStockMovementsUseCase;
import dev.piovra.inventory.application.port.in.StockMovementCommand;
import dev.piovra.inventory.application.port.in.StockMovementMode;
import dev.piovra.model.order.OrderLine;

/**
 * Consumes {@code OrderAccepted} and turns each inventory-affecting line into a DELTA movement.
 *
 * <p>Inert today: nothing publishes {@code OrderAccepted} yet, {@code piovra-order} does not exist.
 * Wired now so that when it does, no further change is needed here.
 */
@Service
public class OrderAcceptedHandler {

    private final ApplyStockMovementsUseCase applyStockMovementsUseCase;

    public OrderAcceptedHandler(ApplyStockMovementsUseCase applyStockMovementsUseCase) {
        this.applyStockMovementsUseCase = applyStockMovementsUseCase;
    }

    @Transactional
    public void handle(OrderAccepted event) {
        List<StockMovementCommand> commands = event.lines().stream()
                .filter(OrderLine::affectsInventory)
                .map(line -> toCommand(event, line))
                .toList();
        if (!commands.isEmpty()) {
            applyStockMovementsUseCase.apply(event.tenantId(), commands);
        }
    }

    private StockMovementCommand toCommand(OrderAccepted event, OrderLine line) {
        int quantity = event.restoresStock() ? line.quantity() : -line.quantity();
        InventoryChanged.Reason reason =
                event.restoresStock() ? InventoryChanged.Reason.RETURN : InventoryChanged.Reason.ORDER;
        String idempotencyKey = Ids.stockMovementKey(
                event.channelId(), event.orderId(), line.lineId(), event.restoresStock() ? "RESTORE" : null);
        return new StockMovementCommand(
                line.sku(), StockMovementMode.DELTA, quantity, reason, event.channelId(), idempotencyKey);
    }
}
