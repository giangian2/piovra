package dev.piovra.connector.woocommerce.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.TenantId;
import dev.piovra.connector.woocommerce.application.port.out.PollCursorStore;
import dev.piovra.connector.woocommerce.application.port.out.PollKind;
import dev.piovra.connector.woocommerce.application.port.out.PollLease;
import dev.piovra.testsupport.PiovraIntegrationTest;

class PollCursorStoreAdapterTest extends PiovraIntegrationTest {

    private static final TenantId TENANT = TenantId.DEFAULT;
    private static final Duration LEASE = Duration.ofMinutes(5);

    @Autowired
    private PollCursorStore store;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void a_free_cursor_row_is_leased_and_a_second_caller_gets_nothing() {
        ChannelId channel = seed(Instant.parse("2026-09-21T10:00:00Z"));

        Optional<PollLease> first = store.acquire(TENANT, channel, PollKind.ORDERS, LEASE);
        Optional<PollLease> second = store.acquire(TENANT, channel, PollKind.ORDERS, LEASE);

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
    }

    @Test
    void the_lease_carries_the_cursor_the_previous_tick_left_behind() {
        Instant cursor = Instant.parse("2026-09-21T10:00:00Z");
        ChannelId channel = seed(cursor);

        PollLease lease = store.acquire(TENANT, channel, PollKind.ORDERS, LEASE).orElseThrow();

        assertThat(lease.cursorAt()).isEqualTo(cursor);
        assertThat(lease.pageCursor()).isNull();
    }

    @Test
    void an_expired_lease_is_acquirable_again() {
        ChannelId channel = seed(Instant.parse("2026-09-21T10:00:00Z"));
        store.acquire(TENANT, channel, PollKind.ORDERS, LEASE).orElseThrow();
        expireLease(channel);

        // A replica that died mid-poll must not hold a channel hostage.
        assertThat(store.acquire(TENANT, channel, PollKind.ORDERS, LEASE)).isPresent();
    }

    @Test
    void advancing_persists_the_high_water_mark() {
        ChannelId channel = seed(Instant.parse("2026-09-21T10:00:00Z"));
        PollLease lease = store.acquire(TENANT, channel, PollKind.ORDERS, LEASE).orElseThrow();
        Instant moved = Instant.parse("2026-09-21T11:30:00Z");

        inTransaction(() -> store.advance(lease, moved, "123:2", LEASE));

        assertThat(cursorAt(channel)).isEqualTo(moved);
        assertThat(pageCursor(channel)).isEqualTo("123:2");
    }

    @Test
    void releasing_after_a_failure_records_the_error_and_leaves_the_cursor_where_it_was() {
        Instant cursor = Instant.parse("2026-09-21T10:00:00Z");
        ChannelId channel = seed(cursor);
        PollLease lease = store.acquire(TENANT, channel, PollKind.ORDERS, LEASE).orElseThrow();

        store.release(lease, "WOO_TRANSIENT_504");

        assertThat(lastError(channel)).isEqualTo("WOO_TRANSIENT_504");
        assertThat(cursorAt(channel)).isEqualTo(cursor);
        assertThat(consecutiveFailures(channel)).isEqualTo(1);
        // And the channel is free again, so the next tick retries it.
        assertThat(store.acquire(TENANT, channel, PollKind.ORDERS, LEASE)).isPresent();
    }

    @Test
    void releasing_after_a_success_clears_the_failure_counter() {
        ChannelId channel = seed(Instant.parse("2026-09-21T10:00:00Z"));
        PollLease lease = store.acquire(TENANT, channel, PollKind.ORDERS, LEASE).orElseThrow();
        store.release(lease, "WOO_TRANSIENT_504");
        PollLease retry = store.acquire(TENANT, channel, PollKind.ORDERS, LEASE).orElseThrow();

        store.release(retry, null);

        assertThat(consecutiveFailures(channel)).isZero();
        assertThat(lastError(channel)).isNull();
    }

    @Test
    void seeding_the_same_channel_twice_leaves_the_cursor_untouched() {
        Instant cursor = Instant.parse("2026-09-21T10:00:00Z");
        ChannelId channel = seed(cursor);

        store.ensureExists(TENANT, channel, PollKind.ORDERS, Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(cursorAt(channel)).isEqualTo(cursor);
    }

    private ChannelId seed(Instant cursorAt) {
        ChannelId channel = ChannelId.of("woo-" + Ids.newId().toLowerCase());
        store.ensureExists(TENANT, channel, PollKind.ORDERS, cursorAt.truncatedTo(ChronoUnit.MICROS));
        return channel;
    }

    private void expireLease(ChannelId channel) {
        jdbcTemplate.update(
                "UPDATE connector_woocommerce.poll_cursor SET leased_until = now() - interval '1 minute'"
                        + " WHERE channel_id = ?",
                channel.value());
    }

    private Instant cursorAt(ChannelId channel) {
        return jdbcTemplate
                .queryForObject(
                        "SELECT cursor_at FROM connector_woocommerce.poll_cursor WHERE channel_id = ?",
                        Timestamp.class,
                        channel.value())
                .toInstant();
    }

    private String pageCursor(ChannelId channel) {
        return jdbcTemplate.queryForObject(
                "SELECT page_cursor FROM connector_woocommerce.poll_cursor WHERE channel_id = ?",
                String.class,
                channel.value());
    }

    private String lastError(ChannelId channel) {
        return jdbcTemplate.queryForObject(
                "SELECT last_error FROM connector_woocommerce.poll_cursor WHERE channel_id = ?",
                String.class,
                channel.value());
    }

    private int consecutiveFailures(ChannelId channel) {
        return jdbcTemplate.queryForObject(
                "SELECT consecutive_failures FROM connector_woocommerce.poll_cursor WHERE channel_id = ?",
                Integer.class,
                channel.value());
    }
}
