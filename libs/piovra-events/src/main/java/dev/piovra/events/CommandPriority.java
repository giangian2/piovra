package dev.piovra.events;

/**
 * Priority of a command towards a channel. Not decorative: it selects the topic, and therefore the
 * queue the command travels on.
 */
public enum CommandPriority {
    /** Stock: oversell risk, small batches, reserved quota. */
    HIGH,
    /** Content, prices, new listings. */
    NORMAL,
    /** Bulk resyncs and reconciliation: must not burn the daily quota in five minutes. */
    LOW
}
