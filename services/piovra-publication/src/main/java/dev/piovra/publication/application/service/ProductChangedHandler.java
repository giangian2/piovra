package dev.piovra.publication.application.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.common.Ids;
import dev.piovra.crosscutting.annotation.Idempotent;
import dev.piovra.events.ChannelCommand;
import dev.piovra.events.ProductChanged;
import dev.piovra.model.channel.ChannelDefinition;
import dev.piovra.outbox.OutboxWriter;
import dev.piovra.publication.application.port.out.ChannelDefinitionCache;
import dev.piovra.publication.application.port.out.ChannelListingRepository;
import dev.piovra.publication.domain.ChannelListing;
import dev.piovra.publication.domain.ChannelProjector;
import dev.piovra.publication.domain.DesiredListing;
import dev.piovra.publication.domain.DiffCalculator;
import dev.piovra.publication.domain.PublicationDecision;

/**
 * For every active channel of the product, projects, diffs and - when the diff says so - emits a
 * command. All the deciding logic ({@link ChannelProjector}, {@link DiffCalculator}) is reused
 * verbatim from the existing domain; this class is wiring only.
 *
 * <p>{@code @Idempotent} lives here rather than on the Kafka adapter deliberately: the consumer calls
 * this class's proxy from the outside, so the annotation actually applies (self-invocation would
 * silently skip it, docs/12-development-guidelines.md section 3.3).
 *
 * <p>No real inventory yet (docs plan, decision 3): {@code availableBySku} is always empty, so every
 * projected quantity is 0. Fine for proving first-publish and no-op; the stock-only/price-only
 * branches stay covered by {@code DiffCalculatorTest} until inventory exists.
 */
@Service
public class ProductChangedHandler {

    private final ChannelDefinitionCache channelDefinitionCache;
    private final ChannelListingRepository channelListingRepository;
    private final ChannelProjector channelProjector;
    private final DiffCalculator diffCalculator;
    private final OutboxWriter outboxWriter;
    private final ObjectMapper objectMapper;
    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {};

    public ProductChangedHandler(
            ChannelDefinitionCache channelDefinitionCache,
            ChannelListingRepository channelListingRepository,
            ChannelProjector channelProjector,
            DiffCalculator diffCalculator,
            OutboxWriter outboxWriter,
            ObjectMapper objectMapper) {
        this.channelDefinitionCache = channelDefinitionCache;
        this.channelListingRepository = channelListingRepository;
        this.channelProjector = channelProjector;
        this.diffCalculator = diffCalculator;
        this.outboxWriter = outboxWriter;
        this.objectMapper = objectMapper;
    }

    @Idempotent(key = "'pc:' + #event.eventId()")
    @Transactional
    public void handle(ProductChanged event) {
        for (ChannelDefinition channel : channelDefinitionCache.activeChannelsFor(event.tenantId())) {
            handleChannel(event, channel);
        }
    }

    private void handleChannel(ProductChanged event, ChannelDefinition channel) {
        DesiredListing desired = channelProjector.project(event.product(), channel, Map.of());
        ChannelListing listing = channelListingRepository
                .find(event.tenantId(), event.sku(), channel.channelId())
                .orElseGet(() -> ChannelListing.notListed(event.tenantId(), event.sku(), channel.channelId()));

        PublicationDecision decision = diffCalculator.decide(desired, listing, event.product(), channel.policy());
        Instant now = Instant.now();

        switch (decision.action()) {
            case PUBLISH -> {
                String commandId = Ids.newId();
                ChannelCommand command = new ChannelCommand(
                        commandId,
                        event.tenantId(),
                        channel.channelId(),
                        channel.type(),
                        event.sku(),
                        decision.operation(),
                        event.revision(),
                        decision.priority(),
                        decision.changedGroups(),
                        objectMapper.convertValue(desired, PAYLOAD_TYPE),
                        0,
                        now);
                channelListingRepository.save(listing.markPending(commandId, now));
                outboxWriter.append(command);
            }
            case BLOCK -> channelListingRepository.save(listing.markBlocked(decision.reason(), now));
            case SKIP -> {
                // Steady state: nothing to persist, nothing to publish.
            }
        }
    }
}
