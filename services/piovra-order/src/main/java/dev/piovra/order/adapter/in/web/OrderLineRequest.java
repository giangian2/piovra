package dev.piovra.order.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import dev.piovra.common.Money;
import dev.piovra.model.order.LineResolution;
import dev.piovra.model.order.OrderLine;

/** Unresolved: {@code resolution}/{@code sku} are assigned by {@code OrderIngestionService}, never
 * by the caller. */
public record OrderLineRequest(
        @NotBlank String lineId,
        String channelLineId,
        @NotBlank String channelSku,
        @Positive int quantity,
        Money unitPrice) {

    public OrderLine toUnresolvedLine() {
        return new OrderLine(lineId, channelLineId, channelSku, null, LineResolution.UNMAPPED, quantity, unitPrice);
    }
}
