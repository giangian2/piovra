package dev.piovra.model.compliance;

/**
 * Legal address of a {@link ComplianceProfile} (manufacturer or responsible person). Deliberately
 * separate from {@code dev.piovra.model.order.Address} (a shipping address): the two are unrelated
 * bounded concepts and the order one already bakes in {@code name}/{@code phone}, which live on
 * {@link ComplianceProfile} here instead.
 */
public record Address(String street, String city, String postalCode, String countryCode) {}
