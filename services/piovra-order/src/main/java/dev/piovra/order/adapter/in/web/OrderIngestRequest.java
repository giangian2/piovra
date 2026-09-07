package dev.piovra.order.adapter.in.web;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.TenantId;
import dev.piovra.model.order.Address;
import dev.piovra.model.order.Buyer;
import dev.piovra.model.order.CanonicalOrder;
import dev.piovra.model.order.OrderStatus;
import dev.piovra.model.order.OrderTotals;

/** Request body of {@code POST /v1/orders}: the manual/ops ingestion path, standing in for a real
 * driver until one exists. {@code orderId} is server-generated, {@code channelId}/{@code
 * channelOrderId} together with the tenant are what dedup is keyed on. */
public record OrderIngestRequest(
        @NotBlank String channelId,
        @NotBlank String channelOrderId,
        @NotNull OrderStatus status,
        String channelStatus,
        @NotNull Instant placedAt,
        @NotNull Buyer buyer,
        @NotNull Address shippingAddress,
        @NotNull OrderTotals totals,
        @NotEmpty List<@Valid OrderLineRequest> lines) {

    public CanonicalOrder toCanonicalOrder(TenantId tenantId) {
        return new CanonicalOrder(
                Ids.newId(),
                tenantId,
                ChannelId.of(channelId),
                channelOrderId,
                status,
                channelStatus,
                placedAt,
                Instant.now(),
                buyer,
                shippingAddress,
                totals,
                lines.stream().map(OrderLineRequest::toUnresolvedLine).toList(),
                false);
    }
}
