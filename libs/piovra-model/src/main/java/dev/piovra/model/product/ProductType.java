package dev.piovra.model.product;

public enum ProductType {
    /** No variants: a single sellable unit, sharing the parent SKU. */
    SIMPLE,
    /** Parent with variants (size, colour...). The revision lives on the parent. */
    VARIANT_PARENT
}
