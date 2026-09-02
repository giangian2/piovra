package dev.piovra.driver.spi;

import dev.piovra.common.Money;
import dev.piovra.common.Sku;

public record PriceUpdate(Sku sku, String externalId, String externalVariantId, Money price, Money compareAtPrice) {}
