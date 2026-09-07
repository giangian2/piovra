package dev.piovra.publication.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.common.ChannelId;
import dev.piovra.common.ErrorClass;
import dev.piovra.common.Ids;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.driver.spi.DriverError;
import dev.piovra.events.ChannelCommand;
import dev.piovra.events.ChannelResult;
import dev.piovra.events.Topics;
import dev.piovra.model.channel.FieldGroup;
import dev.piovra.publication.application.port.out.ChannelListingRepository;
import dev.piovra.publication.domain.ChannelListing;
import dev.piovra.publication.domain.ListingState;
import dev.piovra.testsupport.PiovraIntegrationTest;

/** Closes the loop docs/06-publish-flow.md section 7 describes: before ChannelResultHandler existed,
 * a listing stayed PENDING forever regardless of what the driver actually reported. */
class ChannelResultConsumerIT extends PiovraIntegrationTest {

    private static final TenantId TENANT = TenantId.DEFAULT;
    private static final ChannelId CHANNEL = ChannelId.of("test-channel");

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChannelListingRepository channelListingRepository;

    @Test
    void a_success_outcome_lists_the_product_and_promotes_the_pending_hashes() throws Exception {
        Sku sku = Sku.of("TEST-" + Ids.newId());
        String commandId = Ids.newId();
        Map<FieldGroup, String> pendingHashes = Map.of(FieldGroup.CONTENT, "hash-content");
        channelListingRepository.save(ChannelListing.notListed(TENANT, sku, CHANNEL)
                .markPending(commandId, ChannelCommand.Operation.UPSERT, pendingHashes, Instant.now()));

        ChannelCommand command = command(commandId, sku);
        ChannelResult result = ChannelResult.success(command, "EXT-1", Map.of(), "snapshot-hash", 123L);

        send(result);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            ChannelListing listing =
                    channelListingRepository.find(TENANT, sku, CHANNEL).orElseThrow();
            assertThat(listing.state()).isEqualTo(ListingState.LISTED);
            assertThat(listing.externalId()).isEqualTo("EXT-1");
            assertThat(listing.fieldHashes()).isEqualTo(pendingHashes);
            assertThat(listing.pendingOperation()).isNull();
        });
    }

    @Test
    void a_permanent_error_outcome_marks_the_listing_in_error_and_increments_retry_count() throws Exception {
        Sku sku = Sku.of("TEST-" + Ids.newId());
        String commandId = Ids.newId();
        channelListingRepository.save(ChannelListing.notListed(TENANT, sku, CHANNEL)
                .markPending(
                        commandId, ChannelCommand.Operation.UPSERT, Map.of(FieldGroup.CONTENT, "h"), Instant.now()));

        ChannelCommand command = command(commandId, sku);
        ChannelResult result = ChannelResult.failure(
                command, DriverError.of(ErrorClass.MARKETPLACE_REJECT, "REJECTED", "bad category"), 50L);

        send(result);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            ChannelListing listing =
                    channelListingRepository.find(TENANT, sku, CHANNEL).orElseThrow();
            assertThat(listing.state()).isEqualTo(ListingState.ERROR);
            assertThat(listing.lastErrorCode()).isEqualTo("REJECTED");
            assertThat(listing.retryCount()).isEqualTo(1);
        });
    }

    @Test
    void redelivering_the_same_result_does_not_double_count_the_retry() throws Exception {
        Sku sku = Sku.of("TEST-" + Ids.newId());
        String commandId = Ids.newId();
        channelListingRepository.save(ChannelListing.notListed(TENANT, sku, CHANNEL)
                .markPending(
                        commandId, ChannelCommand.Operation.UPSERT, Map.of(FieldGroup.CONTENT, "h"), Instant.now()));

        ChannelCommand command = command(commandId, sku);
        ChannelResult result = ChannelResult.failure(
                command, DriverError.of(ErrorClass.MARKETPLACE_REJECT, "REJECTED", "bad category"), 50L);

        send(result);
        send(result);

        await().pollDelay(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            ChannelListing listing =
                    channelListingRepository.find(TENANT, sku, CHANNEL).orElseThrow();
            assertThat(listing.retryCount()).isEqualTo(1);
        });
    }

    private static ChannelCommand command(String commandId, Sku sku) {
        return new ChannelCommand(
                commandId,
                TENANT,
                CHANNEL,
                null,
                sku,
                ChannelCommand.Operation.UPSERT,
                1,
                null,
                Set.of(),
                Map.of(),
                0,
                Instant.now());
    }

    private void send(ChannelResult result) throws Exception {
        kafkaTemplate
                .send(Topics.CHANNEL_RESULT, result.sku().value(), objectMapper.writeValueAsString(result))
                .get();
    }
}
