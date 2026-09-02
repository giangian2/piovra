package dev.piovra.publication.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Money;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.events.ChannelCommand;
import dev.piovra.events.CommandPriority;
import dev.piovra.model.channel.ChannelDefinition;
import dev.piovra.model.channel.ChannelPolicy;
import dev.piovra.model.channel.ChannelType;
import dev.piovra.model.channel.FieldGroup;
import dev.piovra.model.product.CanonicalProduct;
import dev.piovra.model.product.CanonicalVariant;
import dev.piovra.model.product.Identifiers;
import dev.piovra.model.product.LocalizedText;
import dev.piovra.model.product.ProductStatus;
import dev.piovra.model.product.ProductType;

/**
 * The diff is the logic the system's throughput rests on: these tests do not check that it
 * compiles, they check that an identical feed produces not a single API call.
 */
class DiffCalculatorTest {

    private static final TenantId TENANT = TenantId.of("acme");
    private static final Sku SKU = Sku.of("TSHIRT-BASE");
    private static final ChannelId CHANNEL = ChannelId.of("woo-main");

    private final FieldGroupHasher hasher = new FieldGroupHasher();
    private final DiffCalculator diff = new DiffCalculator(hasher);
    private final ChannelProjector projector = new ChannelProjector(Locale.ITALIAN);

    @Test
    void first_publish_sends_everything() {
        DesiredListing desired = project(product(1, "T-shirt", "19.90"), 10);
        ChannelListing listing = ChannelListing.notListed(TENANT, SKU, CHANNEL);

        PublicationDecision decision = diff.decide(desired, listing, product(1, "T-shirt", "19.90"), policy());

        assertThat(decision.shouldPublish()).isTrue();
        assertThat(decision.operation()).isEqualTo(ChannelCommand.Operation.UPSERT);
        assertThat(decision.changedGroups()).containsExactlyInAnyOrder(FieldGroup.values());
    }

    @Test
    void an_identical_feed_produces_no_call_at_all() {
        CanonicalProduct p = product(1, "T-shirt", "19.90");
        DesiredListing desired = project(p, 10);
        ChannelListing published = publishedWith(desired, 1);

        PublicationDecision decision = diff.decide(desired, published, p, policy());

        assertThat(decision.action()).isEqualTo(PublicationDecision.Action.SKIP);
        assertThat(decision.changedGroups()).isEmpty();
    }

    @Test
    void stock_only_uses_the_light_endpoint_at_high_priority() {
        CanonicalProduct p = product(2, "T-shirt", "19.90");
        ChannelListing published = publishedWith(project(p, 10), 1);

        PublicationDecision decision = diff.decide(project(p, 7), published, p, policy());

        assertThat(decision.operation()).isEqualTo(ChannelCommand.Operation.INVENTORY);
        assertThat(decision.priority()).isEqualTo(CommandPriority.HIGH);
        assertThat(decision.changedGroups()).containsExactly(FieldGroup.STOCK);
    }

    @Test
    void price_only_uses_the_dedicated_endpoint() {
        ChannelListing published = publishedWith(project(product(1, "T-shirt", "19.90"), 10), 1);
        CanonicalProduct repriced = product(2, "T-shirt", "21.90");

        PublicationDecision decision = diff.decide(project(repriced, 10), published, repriced, policy());

        assertThat(decision.operation()).isEqualTo(ChannelCommand.Operation.PRICE);
        assertThat(decision.changedGroups()).containsExactly(FieldGroup.PRICE);
    }

    @Test
    void title_and_stock_together_become_a_high_priority_upsert() {
        ChannelListing published = publishedWith(project(product(1, "T-shirt", "19.90"), 10), 1);
        CanonicalProduct modified = product(2, "T-shirt premium", "19.90");

        PublicationDecision decision = diff.decide(project(modified, 4), published, modified, policy());

        assertThat(decision.operation()).isEqualTo(ChannelCommand.Operation.UPSERT);
        assertThat(decision.priority()).isEqualTo(CommandPriority.HIGH);
        assertThat(decision.changedGroups()).containsExactlyInAnyOrder(FieldGroup.CONTENT, FieldGroup.STOCK);
    }

    @Test
    void a_command_with_a_stale_revision_is_discarded() {
        ChannelListing published = publishedWith(project(product(5, "T-shirt", "19.90"), 10), 5);
        CanonicalProduct old = product(3, "Old T-shirt", "12.00");

        PublicationDecision decision = diff.decide(project(old, 1), published, old, policy());

        assertThat(decision.action()).isEqualTo(PublicationDecision.Action.SKIP);
        assertThat(decision.reason()).contains("stale");
    }

    @Test
    void a_discontinued_product_is_withdrawn() {
        CanonicalProduct p = product(2, "T-shirt", "19.90", ProductStatus.DISCONTINUED);
        ChannelListing published = publishedWith(project(product(1, "T-shirt", "19.90"), 10), 1);

        PublicationDecision decision = diff.decide(project(p, 10), published, p, policy());

        assertThat(decision.operation()).isEqualTo(ChannelCommand.Operation.END);
    }

    @Test
    void the_channel_buffer_is_never_published() {
        ChannelPolicy withBuffer = new ChannelPolicy(3, 99, BigDecimal.ZERO, false, 3, false);
        DesiredListing desired = project(product(1, "T-shirt", "19.90"), 10, withBuffer);

        assertThat(desired.totalQuantity()).isEqualTo(7);
    }

    @Test
    void a_forced_resync_clears_the_hashes_and_republishes_everything() {
        CanonicalProduct p = product(1, "T-shirt", "19.90");
        DesiredListing desired = project(p, 10);
        ChannelListing published = publishedWith(desired, 1).forgetSnapshot();

        PublicationDecision decision = diff.decide(desired, published, p, policy());

        assertThat(decision.shouldPublish()).isTrue();
        assertThat(decision.changedGroups()).containsExactlyInAnyOrder(FieldGroup.values());
    }

    // ------------------------------------------------------------------ helpers

    private static ChannelPolicy policy() {
        return ChannelPolicy.DEFAULT;
    }

    private DesiredListing project(CanonicalProduct p, int available) {
        return project(p, available, policy());
    }

    private DesiredListing project(CanonicalProduct p, int available, ChannelPolicy policy) {
        ChannelDefinition channel = new ChannelDefinition(
                TENANT,
                CHANNEL,
                ChannelType.WOOCOMMERCE,
                "https://shop.test",
                true,
                "vault://x",
                policy,
                Map.of(),
                Map.of());
        return projector.project(p, channel, Map.of(p.variants().getFirst().sku(), available));
    }

    private ChannelListing publishedWith(DesiredListing desired, long revision) {
        return ChannelListing.notListed(TENANT, SKU, CHANNEL)
                .markPublished("EXT-1", Map.of(), revision, hasher.hashAll(desired), Instant.now());
    }

    private static CanonicalProduct product(long revision, String title, String price) {
        return product(revision, title, price, ProductStatus.ACTIVE);
    }

    private static CanonicalProduct product(long revision, String title, String price, ProductStatus status) {
        return new CanonicalProduct(
                TENANT,
                SKU,
                revision,
                status,
                ProductType.SIMPLE,
                LocalizedText.it(title),
                LocalizedText.it("description"),
                "Acme",
                List.of("Clothing", "T-shirts"),
                Identifiers.EMPTY,
                List.of(),
                Map.of(),
                List.of(),
                List.of(CanonicalVariant.simple(SKU, Money.euro(price))),
                Map.of(),
                Instant.parse("2026-09-01T00:00:00Z"));
    }
}
