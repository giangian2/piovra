package dev.piovra.inventory.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.common.Ids;
import dev.piovra.common.TenantId;
import dev.piovra.events.ProductChanged;
import dev.piovra.events.Topics;
import dev.piovra.inventory.adapter.out.persistence.StockLevelEntity;
import dev.piovra.inventory.adapter.out.persistence.StockLevelJpaRepository;
import dev.piovra.model.product.CanonicalProduct;
import dev.piovra.testsupport.CanonicalProductFixtures;
import dev.piovra.testsupport.PiovraIntegrationTest;

class ProductChangedConsumerIT extends PiovraIntegrationTest {

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /** A plain read, unlike {@code StockLevelRepositoryAdapter.lockOrCreate}: asserting through it
     * would trivially pass regardless of whether the consumer actually created the row. */
    @Autowired
    private StockLevelJpaRepository stockLevelJpaRepository;

    @Test
    void a_product_changed_message_creates_a_zeroed_stock_level_for_every_variant_sku() throws Exception {
        CanonicalProduct product = CanonicalProductFixtures.simpleProduct("TEST-" + Ids.newId());
        ProductChanged event = ProductChanged.created(product);
        String sku = product.variants().getFirst().sku().value();

        kafkaTemplate
                .send(Topics.CATALOG_PRODUCT_CHANGED, event.partitionKey(), objectMapper.writeValueAsString(event))
                .get();

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<StockLevelEntity> entity =
                    stockLevelJpaRepository.findByTenantIdAndSku(TenantId.DEFAULT.value(), sku);
            assertThat(entity).isPresent();
            assertThat(entity.orElseThrow().onHand()).isZero();
        });
    }
}
