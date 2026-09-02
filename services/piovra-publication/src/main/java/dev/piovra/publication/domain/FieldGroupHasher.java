package dev.piovra.publication.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

import dev.piovra.model.channel.FieldGroup;
import dev.piovra.model.product.Media;

/**
 * Computes one hash per field group of the desired payload.
 *
 * <p>The serialization is <b>explicit and not delegated to Jackson</b>: an ObjectMapper's field
 * order can change with a library version or a configuration setting, and at that moment every
 * product in the catalog would look "modified" and be republished everywhere. A hand-written format
 * is ugly but does not do that kind of damage.
 *
 * <p>Every value enters in normalized form: maps are sorted, prices go through
 * {@link dev.piovra.common.Money} which pins the scale, nulls become the empty string.
 */
public final class FieldGroupHasher {

    /** Field separator: US (unit separator), impossible to encounter in product data. */
    private static final char SEP = (char) 0x1F;

    public Map<FieldGroup, String> hashAll(DesiredListing listing) {
        Map<FieldGroup, String> hashes = new EnumMap<>(FieldGroup.class);
        for (FieldGroup group : FieldGroup.values()) {
            hashes.put(group, sha256(canonicalString(listing, group)));
        }
        return Map.copyOf(hashes);
    }

    String canonicalString(DesiredListing l, FieldGroup group) {
        StringBuilder sb = new StringBuilder(256);
        switch (group) {
            case STOCK ->
                l.variants().forEach(v -> sb.append(v.sku())
                        .append('=')
                        .append(v.quantity())
                        .append(SEP));
            case PRICE ->
                l.variants().forEach(v -> sb.append(v.sku())
                        .append('=')
                        .append(v.price())
                        .append('/')
                        .append(v.compareAtPrice() == null ? "" : v.compareAtPrice())
                        .append(SEP));
            case CONTENT -> {
                sb.append(nz(l.title())).append(SEP);
                sb.append(nz(l.description())).append(SEP);
                sb.append(nz(l.brand())).append(SEP);
                sb.append(nz(l.channelCategoryId())).append(SEP);
                appendSorted(sb, l.attributes());
                l.variants().forEach(v -> {
                    sb.append(v.sku()).append('=');
                    appendSorted(sb, v.axisValues());
                    sb.append(SEP);
                });
            }
            case MEDIA -> l.media().forEach(m -> sb.append(mediaFingerprint(m)).append(SEP));
            case SHIPPING ->
                l.variants().forEach(v -> sb.append(v.sku())
                        .append('=')
                        .append(v.weightGrams() == null ? "" : v.weightGrams())
                        .append('/')
                        .append(v.dimensions() == null ? "" : v.dimensions())
                        .append(SEP));
        }
        return sb.toString();
    }

    /**
     * For images what counts is the content, not the URL: switching CDN must not force a reupload of
     * the whole gallery to every marketplace. When the content hash has not been computed yet we
     * fall back to the URL, accepting a few extra reuploads.
     */
    private String mediaFingerprint(Media m) {
        String identity = m.contentHash() != null ? m.contentHash() : m.url();
        return m.role() + ":" + m.position() + ":" + nz(identity);
    }

    private static void appendSorted(StringBuilder sb, Map<String, String> map) {
        new TreeMap<>(map)
                .forEach((k, v) -> sb.append(k).append('=').append(nz(v)).append(','));
    }

    private static String nz(Object s) {
        return s == null ? "" : s.toString();
    }

    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
