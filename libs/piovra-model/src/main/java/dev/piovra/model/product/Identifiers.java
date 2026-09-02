package dev.piovra.model.product;

import java.util.Optional;

/** Commercial identifiers. Also used to resolve order SKUs when the code does not match. */
public record Identifiers(String ean, String upc, String isbn, String mpn, String gtin) {

    public static final Identifiers EMPTY = new Identifiers(null, null, null, null, null);

    public Optional<String> anyGtin() {
        return Optional.ofNullable(gtin != null ? gtin : ean != null ? ean : upc);
    }
}
