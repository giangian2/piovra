package dev.piovra.connector.woocommerce.application.port.out;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import dev.piovra.common.ChannelId;
import dev.piovra.common.TenantId;

/**
 * The polling cursor and the cross-replica lease, which live in the same row because the row has to
 * exist anyway to persist the cursor.
 *
 * <p>{@link #advance} deliberately joins the caller's transaction: the orders a page produced and
 * the cursor that says they were read must commit together, or a crash in between loses orders the
 * marketplace will never offer again.
 */
public interface PollCursorStore {

    /** Creates the row if the channel is new, with the cursor starting at {@code startAt}. */
    void ensureExists(TenantId tenantId, ChannelId channelId, PollKind kind, Instant startAt);

    /** @return empty when another replica holds the channel, or the row does not exist yet */
    Optional<PollLease> acquire(TenantId tenantId, ChannelId channelId, PollKind kind, Duration lease);

    /** Moves the high-water mark and renews the lease. Runs inside the caller's transaction. */
    void advance(PollLease lease, Instant cursorAt, String pageCursor, Duration leaseExtension);

    /** @param errorCode null on success, which is also what resets the failure counter */
    void release(PollLease lease, String errorCode);
}
