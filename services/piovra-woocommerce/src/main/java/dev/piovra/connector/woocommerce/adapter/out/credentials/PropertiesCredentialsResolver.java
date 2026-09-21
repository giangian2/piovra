package dev.piovra.connector.woocommerce.adapter.out.credentials;

import org.springframework.stereotype.Component;

import dev.piovra.common.ErrorClass;
import dev.piovra.common.PiovraException;
import dev.piovra.connector.woocommerce.application.port.out.CredentialsResolver;
import dev.piovra.connector.woocommerce.config.ConnectorProperties;
import dev.piovra.driver.spi.ChannelCredentials;
import dev.piovra.model.channel.ChannelDefinition;

/**
 * Reads the consumer key/secret from configuration, keyed by channelId.
 *
 * <p>A stand-in for Vault, which {@code ChannelDefinition.credentialsRef} already points at and
 * which is nowhere in the stack yet (docs/08-marketplace-drivers.md section 3.3). Swapping it means
 * replacing this one class.
 */
@Component
public class PropertiesCredentialsResolver implements CredentialsResolver {

    private final ConnectorProperties properties;

    public PropertiesCredentialsResolver(ConnectorProperties properties) {
        this.properties = properties;
    }

    @Override
    public ChannelCredentials resolve(ChannelDefinition definition) {
        String channelId = definition.channelId().value();
        ConnectorProperties.Credentials credentials = properties.credentials().get(channelId);
        if (credentials == null) {
            throw new PiovraException(
                    ErrorClass.AUTH,
                    "CONNECTOR_CREDENTIALS_MISSING",
                    "no credentials configured for channel " + channelId + ": set piovra.connector.credentials."
                            + channelId + ".consumer-key and .consumer-secret");
        }
        return ChannelCredentials.basic(credentials.consumerKey(), credentials.consumerSecret());
    }
}
