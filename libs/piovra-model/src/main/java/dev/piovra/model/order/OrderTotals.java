package dev.piovra.model.order;

import dev.piovra.common.Money;

public record OrderTotals(Money items, Money shipping, Money tax, Money grandTotal) {}
