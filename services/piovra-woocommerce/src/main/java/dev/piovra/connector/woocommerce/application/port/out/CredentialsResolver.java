package dev.piovra.connector.woocommerce.application.port.out;

import dev.piovra.driver.spi.ChannelCredentials;
import dev.piovra.model.channel.ChannelDefinition;

/**
 * Turns a channel's {@code credentialsRef} into usable credentials. Resolution and renewal are the
 * connector's job, never the driver's: the driver receives them already resolved.
 */
public interface CredentialsResolver {

    ChannelCredentials resolve(ChannelDefinition definition);
}
