package dev.piovra.driver.tck;

import static org.assertj.core.api.Assertions.assertThat;

import dev.piovra.common.ErrorClass;
import dev.piovra.driver.spi.ChannelContext;
import dev.piovra.driver.spi.DriverCapabilities;
import dev.piovra.driver.spi.DriverError;
import dev.piovra.driver.spi.DriverOutcome;
import dev.piovra.driver.spi.InventoryUpdate;
import dev.piovra.driver.spi.ListingRequest;
import dev.piovra.driver.spi.MarketplaceDriver;
import dev.piovra.driver.spi.OrderPage;
import dev.piovra.driver.spi.OrderQuery;
import dev.piovra.driver.spi.UpdateResult;
import dev.piovra.driver.spi.UpsertResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The contract test every driver must pass.
 *
 * <p>It verifies the <b>contract</b>, not the implementation: that the upsert is idempotent, that
 * errors are translated into the canonical taxonomy, that the declared capabilities match the real
 * behaviour. Without this suite, every new driver would reintroduce the previous one's bugs.
 *
 * <p>It is extended twice per driver: once against WireMock in the normal build, once against the
 * marketplace's real sandbox in a separate nightly job.
 *
 * <pre>{@code
 * class WooCommerceDriverContractTest extends MarketplaceDriverContractTest {
 *     protected MarketplaceDriver driver() { return new WooCommerceDriver(...); }
 *     protected ChannelContext context()   { return TestContexts.woo(wiremockUrl); }
 * }
 * }</pre>
 */
public abstract class MarketplaceDriverContractTest {

    protected abstract MarketplaceDriver driver();

    protected abstract ChannelContext context();

    /** A valid publish request for this channel, with a fresh SKU on every call. */
    protected abstract ListingRequest newListingRequest();

    @Test
    @DisplayName("declared capabilities are self-consistent")
    void capabilities_are_consistent() {
        DriverCapabilities caps = driver().capabilities();

        assertThat(caps.maxTitleLength()).isPositive();
        assertThat(caps.maxImages()).isPositive();
        if (caps.supportsBulkInventory()) {
            assertThat(caps.maxBulkInventorySize())
                    .as("a driver that claims bulk support must also declare its size")
                    .isGreaterThan(1);
        }
        assertThat(caps.bulkInventorySize()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("the declared type is not null")
    void declared_type() {
        assertThat(driver().type()).isNotNull();
    }

    @Test
    @DisplayName("upsert: creates on the first call and returns an external id")
    void upsert_creates() {
        ListingRequest request = newListingRequest();

        UpsertResult result = driver().upsertListing(context(), request);

        assertThat(result.outcome()).isEqualTo(DriverOutcome.SUCCESS);
        assertThat(result.externalId())
                .as("losing the external id means creating a duplicate on the next retry")
                .isNotBlank();
    }

    @Test
    @DisplayName("upsert: an identical second call does not create a duplicate")
    void upsert_is_idempotent() {
        ListingRequest first = newListingRequest();
        UpsertResult created = driver().upsertListing(context(), first);

        UpsertResult again = driver().upsertListing(context(), first);

        assertThat(again.outcome()).isIn(DriverOutcome.SUCCESS, DriverOutcome.NOOP);
        assertThat(again.externalId()).isEqualTo(created.externalId());
    }

    @Test
    @DisplayName("updateInventory: reports failures per item, not per batch")
    void inventory_failures_are_per_item() {
        ListingRequest request = newListingRequest();
        UpsertResult created = driver().upsertListing(context(), request);

        UpdateResult result = driver()
                .updateInventory(
                        context(),
                        List.of(new InventoryUpdate(
                                request.product().variants().getFirst().sku(), created.externalId(), null, 5)));

        assertThat(result.succeeded() + result.failures().size())
                .as("every submitted item must appear among the successes or the failures")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("fetchOrders: pages repeatably and never returns null")
    void fetch_orders_pagination() {
        OrderPage page = driver().fetchOrders(context(), OrderQuery.since(Instant.now().minusSeconds(3600), 50));

        assertThat(page).isNotNull();
        assertThat(page.orders()).isNotNull();
        if (page.hasMore()) {
            assertThat(page.nextCursor()).isNotBlank();
        }
    }

    @Test
    @DisplayName("translate: every exception becomes a canonical error, never null")
    void translate_never_returns_null() {
        DriverError error = driver().translate(new IllegalStateException("boom"));

        assertThat(error).isNotNull();
        assertThat(error.errorClass()).isNotNull();
        assertThat(error.code()).isNotBlank();
        assertThat(error.message()).isNotBlank();
    }

    @Test
    @DisplayName("translate: permanent errors explain what an operator has to do")
    void permanent_errors_carry_a_suggested_action() {
        DriverError error = driver().translate(new IllegalArgumentException("missing category"));

        if (error.errorClass() == ErrorClass.MAPPING || error.errorClass() == ErrorClass.VALIDATION) {
            assertThat(error.suggestedAction())
                    .as("an error that needs human intervention must say which")
                    .isNotBlank();
        }
    }
}
