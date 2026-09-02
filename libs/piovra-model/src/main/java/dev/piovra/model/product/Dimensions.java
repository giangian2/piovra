package dev.piovra.model.product;

/** Dimensions in millimetres. The unit is pinned in the canonical model: conversions live in the
 * feed mappers. */
public record Dimensions(int lengthMm, int widthMm, int heightMm) {}
