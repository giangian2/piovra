package dev.piovra.inventory.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record StockLine(
        @NotBlank String sku, @PositiveOrZero int quantity) {}
