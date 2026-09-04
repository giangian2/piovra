package dev.piovra.publication.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import dev.piovra.crosscutting.port.IdempotencyStore;

/**
 * The concrete port that makes {@code @Idempotent} on {@code ProductChangedHandler} actually
 * activate ({@code CrossCuttingAutoConfiguration} only wires the aspect when an
 * {@link IdempotencyStore} bean exists). Plain JDBC rather than a JPA entity: the table has no
 * domain meaning beyond "was this key claimed", so an {@code INSERT ... ON CONFLICT DO NOTHING} is
 * the whole implementation.
 */
@Repository
public class PublicationIdempotencyStore implements IdempotencyStore {

    private final JdbcTemplate jdbcTemplate;

    public PublicationIdempotencyStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean claim(String key, Duration ttl) {
        int rows = jdbcTemplate.update(
                "INSERT INTO publication.idempotency_key (key, claimed_at, expires_at) VALUES (?, ?, ?) ON CONFLICT (key) DO NOTHING",
                key,
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now().plus(ttl)));
        return rows == 1;
    }

    @Override
    public void release(String key) {
        jdbcTemplate.update("DELETE FROM publication.idempotency_key WHERE key = ?", key);
    }
}
