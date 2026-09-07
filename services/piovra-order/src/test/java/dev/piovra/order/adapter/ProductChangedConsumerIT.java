package dev.piovra.order.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.common.Ids;
import dev.piovra.common.TenantId;
import dev.piovra.events.ProductChanged;
import dev.piovra.events.Topics;
import dev.piovra.model.product.CanonicalProduct;
import dev.piovra.order.application.port.out.KnownSkuRepository;
import dev.piovra.testsupport.CanonicalProductFixtures;
import dev.piovra.testsupport.PiovraIntegrationTest;

class ProductChangedConsumerIT extends PiovraIntegrationTest {

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KnownSkuRepository knownSkuRepository;

    @Test
    void a_product_changed_message_registers_every_variant_sku_as_known() throws Exception {
        CanonicalProduct product = CanonicalProductFixtures.simpleProduct("TEST-" + Ids.newId());
        ProductChanged event = ProductChanged.created(product);

        kafkaTemplate
                .send(Topics.CATALOG_PRODUCT_CHANGED, event.partitionKey(), objectMapper.writeValueAsString(event))
                .get();

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(knownSkuRepository.exists(
                        TenantId.DEFAULT, product.variants().getFirst().sku()))
                .isTrue());
    }
}
