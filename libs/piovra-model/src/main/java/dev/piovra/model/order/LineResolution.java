package dev.piovra.model.order;

/** Outcome of resolving a channel SKU to the canonical SKU. */
public enum LineResolution {
    /** Resolved: stock can be decremented. */
    MAPPED,
    /** Unknown SKU. The order is stored anyway; the decrement is applied once the mapping exists. */
    UNMAPPED,
    /** Several canonical products match: a human has to choose. */
    AMBIGUOUS,
    /** Sold on the channel but not managed by Piovra: ignored by inventory. */
    EXTERNAL
}
