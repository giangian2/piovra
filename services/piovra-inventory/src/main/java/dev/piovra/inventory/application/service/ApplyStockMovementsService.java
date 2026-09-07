package dev.piovra.inventory.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.piovra.common.TenantId;
import dev.piovra.events.InventoryChanged;
import dev.piovra.inventory.application.port.in.ApplyStockMovementsUseCase;
import dev.piovra.inventory.application.port.in.StockMovementCommand;
import dev.piovra.inventory.application.port.out.StockLevelRepository;
import dev.piovra.inventory.application.port.out.StockMovementRepository;
import dev.piovra.inventory.domain.model.StockLevel;
import dev.piovra.inventory.domain.service.StockLedger;
import dev.piovra.outbox.OutboxWriter;

/**
 * The one place both write paths (the REST batch and, later, the {@code OrderAccepted} consumer)
 * converge: lock the row, compute the new level in pure code ({@link StockLedger}), append an
 * idempotent movement, and - only if it actually changed something - persist and emit
 * {@link InventoryChanged}.
 *
 * <p>The row lock from {@link StockLevelRepository#lockOrCreate} guards two <em>different</em>
 * concurrent movements from corrupting the arithmetic; the {@code idempotency_key} unique constraint
 * (see {@link StockMovementRepository#append}) guards the <em>same</em> movement being applied twice
 * (docs/07-order-flow.md section 4.1) - the mandatory "receiving the same event twice" case
 * (docs/12-development-guidelines.md section 6).
 */
@Service
public class ApplyStockMovementsService implements ApplyStockMovementsUseCase {

    private final StockLevelRepository stockLevelRepository;
    private final StockMovementRepository stockMovementRepository;
    private final OutboxWriter outboxWriter;

    public ApplyStockMovementsService(
            StockLevelRepository stockLevelRepository,
            StockMovementRepository stockMovementRepository,
            @Qualifier("inventoryOutboxWriter") OutboxWriter outboxWriter) {
        this.stockLevelRepository = stockLevelRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.outboxWriter = outboxWriter;
    }

    @Override
    @Transactional
    public List<InventoryChanged> apply(TenantId tenantId, List<StockMovementCommand> commands) {
        List<InventoryChanged> changes = new ArrayList<>();
        for (StockMovementCommand command : commands) {
            apply(tenantId, command).ifPresent(changes::add);
        }
        return changes;
    }

    private Optional<InventoryChanged> apply(TenantId tenantId, StockMovementCommand command) {
        StockLevel before = stockLevelRepository.lockOrCreate(tenantId, command.sku());
        StockLevel after =
                switch (command.mode()) {
                    case SET -> StockLedger.applySet(before, command.quantity());
                    case DELTA -> StockLedger.applyDelta(before, command.quantity());
                };
        int delta = after.onHand() - before.onHand();

        Optional<Long> movementId = stockMovementRepository.append(
                tenantId, command.sku(), delta, command.reason(), command.sourceChannelId(), command.idempotencyKey());
        if (movementId.isEmpty()) {
            return Optional.empty();
        }

        stockLevelRepository.save(after);
        if (!StockLedger.availableChanged(before, after)) {
            return Optional.empty();
        }

        InventoryChanged event = InventoryChanged.of(
                tenantId,
                command.sku(),
                after.available(),
                before.available(),
                command.reason(),
                command.sourceChannelId(),
                movementId.get());
        outboxWriter.append(event);
        return Optional.of(event);
    }
}
