package dev.piovra.model.product;

public enum ProductStatus {
    /** Publishable. */
    ACTIVE,
    /** Work in progress, not publishable yet. */
    DRAFT,
    /** Out of catalog: triggers delisting on the channels. */
    DISCONTINUED
}
