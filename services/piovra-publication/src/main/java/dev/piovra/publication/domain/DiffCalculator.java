package dev.piovra.publication.domain;

import dev.piovra.events.ChannelCommand;
import dev.piovra.events.CommandPriority;
import dev.piovra.model.channel.ChannelPolicy;
import dev.piovra.model.channel.FieldGroup;
import dev.piovra.model.product.CanonicalProduct;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Decides whether and what to publish. This is the class that determines the system's real
 * throughput: feeds typically resend 95% identical data, and marketplaces limit calls, not CPU.
 * Every no-op recognised here is one API call saved.
 *
 * <p>A pure function, no I/O: it takes the desired state and the published state and returns a
 * decision. Every edge case is covered by unit tests that run without Spring.
 */
public final class DiffCalculator {

    private final FieldGroupHasher hasher;

    public DiffCalculator(FieldGroupHasher hasher) {
        this.hasher = hasher;
    }

    /**
     * @param desired canonical projection for the channel
     * @param listing state of the last successful publish
     * @param product canonical product, for its status (ACTIVE / DISCONTINUED)
     */
    public PublicationDecision decide(
            DesiredListing desired, ChannelListing listing, CanonicalProduct product, ChannelPolicy policy) {

        // 1. The product left the perimeter: withdraw it, if there is anything to withdraw.
        if (!product.isPublishable()) {
            return listing.isPublished()
                    ? PublicationDecision.publish(
                            ChannelCommand.Operation.END,
                            CommandPriority.NORMAL,
                            Set.of(),
                            "product is " + product.status())
                    : PublicationDecision.skip("product not publishable and not published");
        }

        // 2. Stale command: a higher revision has already been published.
        if (listing.isStale(desired.revision())) {
            return PublicationDecision.skip(
                    "stale revision: %d < %d".formatted(desired.revision(), listing.publishedRevision()));
        }

        // 3. Zero quantity with a delist-on-zero policy.
        if (policy.endOnZero() && desired.totalQuantity() == 0 && listing.isPublished()) {
            return PublicationDecision.publish(
                    ChannelCommand.Operation.END,
                    CommandPriority.HIGH,
                    Set.of(FieldGroup.STOCK),
                    "out of stock and endOnZero policy is active");
        }

        Map<FieldGroup, String> desiredHashes = hasher.hashAll(desired);
        Set<FieldGroup> changed = changedGroups(desiredHashes, listing.fieldHashes());

        // 4. First publish: send everything, even though "nothing changed" against an empty state.
        if (!listing.isPublished()) {
            return PublicationDecision.publish(
                    ChannelCommand.Operation.UPSERT,
                    CommandPriority.NORMAL,
                    EnumSet.allOf(FieldGroup.class),
                    "first publish on this channel");
        }

        // 5. The most frequent case at steady state: nothing changed.
        if (changed.isEmpty()) {
            return PublicationDecision.skip("no difference from the published snapshot");
        }

        // 6. Stock only: dedicated endpoint, high priority because this is the oversell risk.
        if (changed.equals(Set.of(FieldGroup.STOCK))) {
            String reason = policy.isCritical(desired.totalQuantity())
                    ? "stock below the critical threshold"
                    : "stock-only change";
            return PublicationDecision.publish(
                    ChannelCommand.Operation.INVENTORY, CommandPriority.HIGH, changed, reason);
        }

        // 7. Price only: dedicated endpoint.
        if (changed.equals(Set.of(FieldGroup.PRICE))) {
            return PublicationDecision.publish(
                    ChannelCommand.Operation.PRICE, CommandPriority.NORMAL, changed, "price-only change");
        }

        // 8. Everything else: a listing update. changedGroups reaches the driver, which will pick
        //    the lightest call that covers them all.
        CommandPriority priority = changed.contains(FieldGroup.STOCK) ? CommandPriority.HIGH : CommandPriority.NORMAL;
        return PublicationDecision.publish(
                ChannelCommand.Operation.UPSERT,
                priority,
                changed,
                "changes in: " + changed.stream().map(Enum::name).sorted().toList());
    }

    /**
     * A group changed when the desired hash differs from the published one. A group missing from the
     * published state counts as changed: that is the forced-resync case, which clears the hashes
     * precisely to obtain this effect.
     */
    Set<FieldGroup> changedGroups(Map<FieldGroup, String> desired, Map<FieldGroup, String> published) {
        Set<FieldGroup> changed = EnumSet.noneOf(FieldGroup.class);
        desired.forEach((group, hash) -> {
            if (!hash.equals(published.get(group))) {
                changed.add(group);
            }
        });
        return changed;
    }
}
