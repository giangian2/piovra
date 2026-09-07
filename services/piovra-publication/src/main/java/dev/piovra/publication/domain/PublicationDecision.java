package dev.piovra.publication.domain;

import java.util.Map;
import java.util.Set;

import dev.piovra.events.ChannelCommand;
import dev.piovra.events.CommandPriority;
import dev.piovra.model.channel.FieldGroup;

/**
 * What to do for a product on a channel.
 *
 * @param changedGroups the groups that actually changed; it travels all the way to the driver so it
 *     can pick the cheapest call
 * @param desiredHashes the full per-group hash of the desired state (every {@link FieldGroup}, not
 *     just the changed ones), carried forward onto {@code ChannelListing.pendingFieldHashes} at
 *     {@code markPending} time so {@code ChannelResultHandler} can promote it into {@code
 *     fieldHashes} once the driver confirms success - empty for a decision that only ends a listing,
 *     since nothing needs to be diffed against next time.
 * @param reason human-readable explanation, surfaced in the console and in the logs
 */
public record PublicationDecision(
        Action action,
        ChannelCommand.Operation operation,
        CommandPriority priority,
        Set<FieldGroup> changedGroups,
        Map<FieldGroup, String> desiredHashes,
        String reason) {

    public PublicationDecision {
        changedGroups = changedGroups == null ? Set.of() : Set.copyOf(changedGroups);
        desiredHashes = desiredHashes == null ? Map.of() : Map.copyOf(desiredHashes);
    }

    public enum Action {
        /** Emit the command. */
        PUBLISH,
        /** Nothing to do: no marketplace call. This is the most frequent outcome at steady state. */
        SKIP,
        /** The channel rules are not satisfied: report it and do not attempt. */
        BLOCK
    }

    public static PublicationDecision skip(String reason) {
        return new PublicationDecision(Action.SKIP, null, null, Set.of(), Map.of(), reason);
    }

    public static PublicationDecision block(String reason) {
        return new PublicationDecision(Action.BLOCK, null, null, Set.of(), Map.of(), reason);
    }

    public static PublicationDecision publish(
            ChannelCommand.Operation op,
            CommandPriority priority,
            Set<FieldGroup> groups,
            Map<FieldGroup, String> desiredHashes,
            String reason) {
        return new PublicationDecision(Action.PUBLISH, op, priority, groups, desiredHashes, reason);
    }

    public boolean shouldPublish() {
        return action == Action.PUBLISH;
    }
}
