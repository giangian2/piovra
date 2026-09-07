package dev.piovra.publication.application.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.piovra.crosscutting.annotation.Idempotent;
import dev.piovra.events.ChannelCommand;
import dev.piovra.events.ChannelResult;
import dev.piovra.publication.application.port.out.ChannelListingRepository;
import dev.piovra.publication.domain.ChannelListing;

/**
 * Closes the driver -> publication loop (docs/06-publish-flow.md section 7): applies the outcome of
 * a command to the listing. Before this class existed, nothing consumed {@code ChannelResult} at
 * all, so {@code channel_listing.state} never left {@code PENDING}.
 *
 * <p>{@code @Idempotent}, same reasoning as {@code ProductChangedHandler}: re-applying an
 * {@code ERROR} outcome twice would double-count {@code retryCount}, and the mandatory "receiving the
 * same event twice" case (docs/12-development-guidelines.md section 6) applies here too.
 *
 * <p>{@code RETRYABLE_ERROR} is treated the same as {@code PERMANENT_ERROR} for now (mark
 * {@code ERROR}, increment {@code retryCount}) rather than routed through a separate retry-topic with
 * backoff (docs/09-errors-observability.md section 2) - that is consumer-side redelivery machinery,
 * a materially larger feature, deliberately out of scope here.
 */
@Service
public class ChannelResultHandler {

    private final ChannelListingRepository channelListingRepository;

    public ChannelResultHandler(ChannelListingRepository channelListingRepository) {
        this.channelListingRepository = channelListingRepository;
    }

    @Idempotent(key = "'cr:' + #result.eventId()")
    @Transactional
    public void handle(ChannelResult result) {
        Optional<ChannelListing> maybeListing =
                channelListingRepository.find(result.tenantId(), result.sku(), result.channelId());
        if (maybeListing.isEmpty()) {
            return;
        }
        ChannelListing listing = maybeListing.get();
        if (listing.lastCommandId() != null && !listing.lastCommandId().equals(result.commandId())) {
            // A result for a command this listing has since moved past: a newer one is already
            // PENDING or resolved. Applying it now would clobber more recent state.
            return;
        }

        Instant now = Instant.now();
        ChannelListing updated =
                switch (result.outcome()) {
                    case SUCCESS ->
                        listing.pendingOperation() == ChannelCommand.Operation.END
                                ? listing.markEnded(now)
                                : listing.markPublished(
                                        result.externalId(),
                                        result.externalVariantIds(),
                                        result.revision(),
                                        listing.pendingFieldHashes(),
                                        now);
                    case NOOP, STALE -> listing;
                    case RETRYABLE_ERROR, PERMANENT_ERROR ->
                        listing.markError(result.error().code(), result.error().message(), now);
                };

        if (updated != listing) {
            channelListingRepository.save(updated);
        }
    }
}
