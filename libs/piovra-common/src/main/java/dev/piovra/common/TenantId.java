package dev.piovra.common;

import java.util.Objects;

/** Owner of the data. Present everywhere from day one: adding it later is expensive. */
public record TenantId(String value) {

    public static final TenantId DEFAULT = new TenantId("default");

    public TenantId {
        Objects.requireNonNull(value, "tenantId");
        value = value.trim().toLowerCase();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("empty tenantId");
        }
    }

    public static TenantId of(String value) {
        return new TenantId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
