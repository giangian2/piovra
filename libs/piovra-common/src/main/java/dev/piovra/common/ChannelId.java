package dev.piovra.common;

import java.util.Objects;

/**
 * Identifies an account on a marketplace, not the marketplace itself: "ebay-it-main" and
 * "ebay-de-outlet" are distinct channels of the same type.
 */
public record ChannelId(String value) {

    public ChannelId {
        Objects.requireNonNull(value, "channelId");
        value = value.trim().toLowerCase();
        if (!value.matches("[a-z0-9][a-z0-9-]{1,62}")) {
            throw new IllegalArgumentException("invalid channelId: " + value);
        }
    }

    public static ChannelId of(String value) {
        return new ChannelId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
