package dev.piovra.common;

import java.util.Objects;

/**
 * Canonical identifier of a sellable unit. It is the Kafka partition key and the natural key of
 * the catalog: see docs/adr/0002-kafka-sku-key.md.
 */
public record Sku(String value) implements Comparable<Sku> {

    private static final int MAX_LENGTH = 100;

    public Sku {
        Objects.requireNonNull(value, "sku");
        value = value.trim().toUpperCase();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("empty sku");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("sku too long: " + value.length() + " > " + MAX_LENGTH);
        }
    }

    public static Sku of(String value) {
        return new Sku(value);
    }

    @Override
    public int compareTo(Sku o) {
        return value.compareTo(o.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
