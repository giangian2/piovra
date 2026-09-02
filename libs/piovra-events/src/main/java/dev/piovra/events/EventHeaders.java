package dev.piovra.events;

/**
 * Standard Kafka headers. Present on EVERY message: they are what makes it possible to answer
 * "what happened to this morning's feed?" without reading payloads.
 */
public final class EventHeaders {

    private EventHeaders() {}

    public static final String EVENT_ID = "x-piovra-event-id";
    /** Correlates the whole chain: starts at the feedId or orderId and reaches the marketplace call. */
    public static final String CORRELATION_ID = "x-piovra-correlation-id";
    /** Id of the event that caused this one. Reconstructs the causal tree. */
    public static final String CAUSATION_ID = "x-piovra-causation-id";

    public static final String TENANT = "x-piovra-tenant";
    public static final String SCHEMA_VERSION = "x-piovra-schema-version";
    public static final String CHANNEL_ID = "x-piovra-channel-id";

    /** Added only to messages that end up in a DLQ. */
    public static final String ERROR_CLASS = "x-piovra-error-class";

    public static final String ERROR_CODE = "x-piovra-error-code";
    public static final String ERROR_MESSAGE = "x-piovra-error-message";
    public static final String ORIGINAL_TOPIC = "x-piovra-original-topic";
    public static final String ORIGINAL_PARTITION = "x-piovra-original-partition";
    public static final String ORIGINAL_OFFSET = "x-piovra-original-offset";
    public static final String ATTEMPTS = "x-piovra-attempts";
    public static final String FAILED_AT = "x-piovra-failed-at";

    /** W3C trace context: propagated in headers so the trace survives Kafka. */
    public static final String TRACEPARENT = "traceparent";
}
