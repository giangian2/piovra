package dev.piovra.inventory.application.port.in;

public enum StockMovementMode {
    /** Absolute: the new on-hand quantity. Used by the manual/feed write path (reason FEED_SET). */
    SET,
    /** Relative: signed change to on-hand. Used by order-driven movements (reason ORDER/RETURN). */
    DELTA
}
