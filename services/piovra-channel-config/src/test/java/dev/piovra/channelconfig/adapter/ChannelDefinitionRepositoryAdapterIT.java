package dev.piovra.channelconfig.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.piovra.channelconfig.adapter.out.persistence.ChannelDefinitionRepositoryAdapter;
import dev.piovra.common.Ids;
import dev.piovra.model.channel.ChannelDefinition;
import dev.piovra.testsupport.ChannelDefinitionFixtures;
import dev.piovra.testsupport.PiovraIntegrationTest;

class ChannelDefinitionRepositoryAdapterIT extends PiovraIntegrationTest {

    @Autowired
    private ChannelDefinitionRepositoryAdapter adapter;

    @Test
    void round_trips_a_channel_definition() {
        ChannelDefinition definition =
                ChannelDefinitionFixtures.channel("test-" + Ids.newId().toLowerCase());

        adapter.save(definition);
        Optional<ChannelDefinition> found = adapter.findByChannelId(definition.tenantId(), definition.channelId());

        assertThat(found).contains(definition);
    }

    @Test
    void a_second_save_updates_the_same_row_instead_of_creating_a_new_one() {
        ChannelDefinition definition =
                ChannelDefinitionFixtures.channel("test-" + Ids.newId().toLowerCase());
        adapter.save(definition);

        ChannelDefinition disabled = new ChannelDefinition(
                definition.tenantId(),
                definition.channelId(),
                definition.type(),
                definition.marketplaceCode(),
                false,
                definition.credentialsRef(),
                definition.policy(),
                definition.categoryMapping(),
                definition.settings());
        adapter.save(disabled);

        assertThat(adapter.findByTenant(definition.tenantId()))
                .filteredOn(d -> d.channelId().equals(definition.channelId()))
                .hasSize(1)
                .first()
                .extracting(ChannelDefinition::enabled)
                .isEqualTo(false);
    }
}
