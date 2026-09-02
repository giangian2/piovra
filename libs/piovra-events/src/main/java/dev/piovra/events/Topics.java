package dev.piovra.events;

import dev.piovra.model.channel.ChannelType;
import java.util.Locale;

/**
 * Topic names in a single place. Convention: piovra.&lt;domain&gt;.&lt;entity&gt;.&lt;verb&gt;.v&lt;n&gt;
 *
 * <p>The version is part of the name: a breaking change creates a .v2 topic and migrates the
 * consumers rather than breaking .v1 (docs/04-kafka-events.md section 8).
 */
public final class Topics {

    private Topics() {}

    public static final String FEED_RECEIVED = "piovra.feed.received.v1";
    public static final String FEED_RECORD_REJECTED = "piovra.feed.record.rejected.v1";
    public static final String FEED_COMPLETED = "piovra.feed.completed.v1";

    public static final String CATALOG_PRODUCT_UPSERT = "piovra.catalog.product.upsert.v1";
    public static final String CATALOG_PRODUCT_CHANGED = "piovra.catalog.product.changed.v1";
    public static final String CATALOG_PRODUCT_SNAPSHOT = "piovra.catalog.product.snapshot.v1";

    public static final String INVENTORY_CHANGED = "piovra.inventory.changed.v1";

    public static final String CHANNEL_RESULT = "piovra.channel.result.v1";
    public static final String CHANNEL_ORDER_RECEIVED = "piovra.channel.order.received.v1";
    public static final String CHANNEL_CONFIG = "piovra.channel.config.v1";

    public static final String ORDER_ACCEPTED = "piovra.order.accepted.v1";
    public static final String ORDER_STATUS_CHANGED = "piovra.order.status.changed.v1";

    /**
     * One command topic per channel type and per priority (docs/adr/0003-topic-per-channel.md).
     * Kafka has no priority within a partition: separate topics are the only way to let stock
     * updates overtake everything else.
     */
    public static String channelCommand(ChannelType type, CommandPriority priority) {
        return "piovra.channel.command.%s.%s.v1"
                .formatted(type.name().toLowerCase(Locale.ROOT), priority.name().toLowerCase(Locale.ROOT));
    }

    /** Delay topic: the consumer waits for the deadline and republishes to the original topic. */
    public static String retry(String topic, String delayLabel) {
        return topic.replaceFirst("\\.v(\\d+)$", ".retry." + delayLabel + ".v$1");
    }

    public static String dlq(String topic) {
        return topic.replaceFirst("\\.v(\\d+)$", ".dlq.v$1");
    }
}
