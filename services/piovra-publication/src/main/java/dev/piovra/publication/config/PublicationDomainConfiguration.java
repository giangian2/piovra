package dev.piovra.publication.config;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.piovra.publication.domain.ChannelProjector;
import dev.piovra.publication.domain.DiffCalculator;
import dev.piovra.publication.domain.FieldGroupHasher;

/** Beans for the existing, untouched domain classes: they take plain constructor arguments, not
 * Spring wiring, so this is the only place that turns them into beans. */
@Configuration(proxyBeanMethods = false)
public class PublicationDomainConfiguration {

    @Bean
    public FieldGroupHasher fieldGroupHasher() {
        return new FieldGroupHasher();
    }

    @Bean
    public DiffCalculator diffCalculator(FieldGroupHasher hasher) {
        return new DiffCalculator(hasher);
    }

    @Bean
    public ChannelProjector channelProjector(@Value("${piovra.publication.locale:it}") String locale) {
        return new ChannelProjector(Locale.forLanguageTag(locale));
    }
}
