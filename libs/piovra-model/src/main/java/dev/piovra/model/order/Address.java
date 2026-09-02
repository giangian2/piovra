package dev.piovra.model.order;

/** Shipping address. Contains personal data: never in logs, masked in the console. */
public record Address(
        String name,
        String line1,
        String line2,
        String city,
        String province,
        String postalCode,
        String countryCode,
        String phone) {}
