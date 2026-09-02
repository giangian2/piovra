package dev.piovra.model.product;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Text per language. Localized from the start even though phase 1 only uses one locale:
 * introducing it later means touching every mapper and every driver.
 */
public record LocalizedText(Map<String, String> values) {

    public LocalizedText {
        values = values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static LocalizedText of(String language, String text) {
        return new LocalizedText(Map.of(language, text));
    }

    public static LocalizedText it(String text) {
        return of("it", text);
    }

    public Optional<String> get(String language) {
        return Optional.ofNullable(values.get(language));
    }

    /** Text for the requested language, falling back to the first available: a driver must never
     * receive null. */
    public Optional<String> resolve(Locale locale) {
        return get(locale.getLanguage()).or(() -> values.values().stream().findFirst());
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
