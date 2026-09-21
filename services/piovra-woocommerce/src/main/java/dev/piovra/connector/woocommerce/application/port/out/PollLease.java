package dev.piovra.connector.woocommerce.application.port.out;

import java.time.Instant;

import dev.piovra.common.ChannelId;
import dev.piovra.common.TenantId;

/**
 * Proof that this instance currently owns a channel's polling, plus the cursor it inherited.
 *
 * @param cursorAt high-water mark on the marketplace's modification timestamp
 * @param pageCursor the driver's own pagination token when a previous tick stopped mid-scan
 */
public record PollLease(TenantId tenantId, ChannelId channelId, PollKind kind, Instant cursorAt, String pageCursor) {}
