package dev.piovra.connector.woocommerce.application.port.out;

import dev.piovra.common.ChannelId;
import dev.piovra.common.TenantId;

/**
 * Archives the marketplace's original payload and hands back a reference to it.
 *
 * <p>The payload carries the buyer's name, address and email. It goes to object storage and only
 * its URI travels on Kafka: a topic is readable by every consumer of it, and an address is not
 * everyone's business (docs/07-order-flow.md).
 */
public interface RawPayloadStore {

    String store(TenantId tenantId, ChannelId channelId, String channelOrderId, String payload);
}
