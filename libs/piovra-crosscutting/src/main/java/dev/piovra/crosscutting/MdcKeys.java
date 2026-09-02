package dev.piovra.crosscutting;

/**
 * Logging context keys. In one place, because a key typed by hand does not fail: the log simply
 * stops being correlatable, and nobody notices until it actually matters.
 *
 * <p>With virtual threads the MDC keeps working as it always has, which is one of the reasons we do
 * not use reactive (docs/10-stack-and-repo.md).
 */
public final class MdcKeys {

    private MdcKeys() {}

    public static final String TENANT = "tenantId";
    public static final String SKU = "sku";
    public static final String CHANNEL = "channelId";
    public static final String COMMAND_ID = "commandId";
    public static final String ORDER_ID = "orderId";
    public static final String FEED_ID = "feedId";
    public static final String CORRELATION_ID = "correlationId";
}
