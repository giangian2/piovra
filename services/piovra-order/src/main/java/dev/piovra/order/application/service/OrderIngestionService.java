package dev.piovra.order.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.piovra.common.Ids;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.events.OrderAccepted;
import dev.piovra.events.OrderStatusChanged;
import dev.piovra.model.order.CanonicalOrder;
import dev.piovra.model.order.OrderLine;
import dev.piovra.model.order.OrderStatus;
import dev.piovra.order.application.port.in.FindOrderUseCase;
import dev.piovra.order.application.port.in.IngestOrderUseCase;
import dev.piovra.order.application.port.out.KnownSkuRepository;
import dev.piovra.order.application.port.out.OrderRepository;
import dev.piovra.order.domain.service.LineResolver;
import dev.piovra.outbox.OutboxWriter;

/**
 * Dedups on {@code (tenantId, channelId, channelOrderId)} (docs/07-order-flow.md section 3): a
 * resend with the same status is a no-op; a resend with a different status re-resolves the lines
 * against the current known-SKU state (a UNMAPPED line becomes MAPPED here if a mapping has since
 * appeared - the only retroactive-resolution trigger this iteration has), persists, emits {@link
 * OrderStatusChanged}, and re-emits {@link OrderAccepted} so inventory applies the resulting
 * movement (e.g. a positive one on CANCELLED/REFUNDED, via the same idempotency key with a RESTORE
 * suffix - see {@code OrderAcceptedHandler} in piovra-inventory).
 */
@Service
public class OrderIngestionService implements IngestOrderUseCase, FindOrderUseCase {

    private final OrderRepository orderRepository;
    private final KnownSkuRepository knownSkuRepository;
    private final OutboxWriter outboxWriter;

    public OrderIngestionService(
            OrderRepository orderRepository,
            KnownSkuRepository knownSkuRepository,
            @Qualifier("orderOutboxWriter") OutboxWriter outboxWriter) {
        this.orderRepository = orderRepository;
        this.knownSkuRepository = knownSkuRepository;
        this.outboxWriter = outboxWriter;
    }

    @Override
    @Transactional
    public CanonicalOrder ingest(CanonicalOrder incomingOrder, String rawPayloadUri) {
        Optional<CanonicalOrder> existing = orderRepository.findByChannelOrderId(
                incomingOrder.tenantId(), incomingOrder.channelId(), incomingOrder.channelOrderId());

        if (existing.isEmpty()) {
            return acceptNewOrder(incomingOrder);
        }

        CanonicalOrder current = existing.get();
        if (current.status() == incomingOrder.status()) {
            return current;
        }
        return updateStatus(current, incomingOrder.status());
    }

    @Override
    public Optional<CanonicalOrder> find(TenantId tenantId, String orderId) {
        return orderRepository.findByOrderId(tenantId, orderId);
    }

    private CanonicalOrder acceptNewOrder(CanonicalOrder incoming) {
        List<OrderLine> resolved = resolveLines(incoming.tenantId(), incoming.lines());
        CanonicalOrder order = new CanonicalOrder(
                Ids.newId(),
                incoming.tenantId(),
                incoming.channelId(),
                incoming.channelOrderId(),
                incoming.status(),
                incoming.channelStatus(),
                incoming.placedAt(),
                Instant.now(),
                incoming.buyer(),
                incoming.shippingAddress(),
                incoming.totals(),
                resolved,
                false);
        CanonicalOrder saved = orderRepository.save(order);
        emitAcceptedIfNeeded(saved);
        return saved;
    }

    private CanonicalOrder updateStatus(CanonicalOrder current, OrderStatus newStatus) {
        List<OrderLine> resolved = resolveLines(current.tenantId(), current.lines());
        CanonicalOrder updated = new CanonicalOrder(
                current.orderId(),
                current.tenantId(),
                current.channelId(),
                current.channelOrderId(),
                newStatus,
                current.channelStatus(),
                current.placedAt(),
                Instant.now(),
                current.buyer(),
                current.shippingAddress(),
                current.totals(),
                resolved,
                current.stockApplied());
        CanonicalOrder saved = orderRepository.save(updated);
        outboxWriter.append(OrderStatusChanged.of(current, newStatus));
        emitAcceptedIfNeeded(saved);
        return saved;
    }

    /** Optimistic: there is no feedback loop from inventory back to order-service, so this is set
     * true as soon as OrderAccepted is queued, not once inventory confirms the movement. */
    private void emitAcceptedIfNeeded(CanonicalOrder order) {
        if (!order.inventoryAffectingLines().isEmpty()) {
            outboxWriter.append(OrderAccepted.from(order));
        }
    }

    private List<OrderLine> resolveLines(TenantId tenantId, List<OrderLine> lines) {
        return lines.stream().map(line -> resolveLine(tenantId, line)).toList();
    }

    private OrderLine resolveLine(TenantId tenantId, OrderLine raw) {
        Optional<Sku> resolvedSku = parseSku(raw.channelSku()).filter(sku -> knownSkuRepository.exists(tenantId, sku));
        return LineResolver.resolve(raw, resolvedSku);
    }

    private static Optional<Sku> parseSku(String value) {
        try {
            return Optional.of(Sku.of(value));
        } catch (IllegalArgumentException invalidSku) {
            return Optional.empty();
        }
    }
}
