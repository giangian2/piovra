package dev.piovra.publication.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Ids;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.events.InventoryChanged;
import dev.piovra.events.Topics;
import dev.piovra.publication.application.port.out.InventoryCache;
import dev.piovra.testsupport.PiovraIntegrationTest;

class InventoryChangedConsumerIT extends PiovraIntegrationTest {

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryCache inventoryCache;

    @Test
    void an_inventory_changed_message_updates_the_local_cache() throws Exception {
        TenantId tenant = TenantId.DEFAULT;
        Sku sku = Sku.of("TEST-" + Ids.newId());
        InventoryChanged event =
                InventoryChanged.of(tenant, sku, 7, 0, InventoryChanged.Reason.FEED_SET, (ChannelId) null, 1L);

        kafkaTemplate
                .send(Topics.INVENTORY_CHANGED, event.partitionKey(), objectMapper.writeValueAsString(event))
                .get();

        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(inventoryCache.availableFor(tenant, List.of(sku)))
                        .containsEntry(sku, 7));
    }
}
