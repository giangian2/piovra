package dev.piovra.catalog.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.piovra.catalog.adapter.out.persistence.ProductRepositoryAdapter;
import dev.piovra.common.Ids;
import dev.piovra.common.Money;
import dev.piovra.model.product.CanonicalProduct;
import dev.piovra.testsupport.CanonicalProductFixtures;
import dev.piovra.testsupport.PiovraIntegrationTest;

class ProductRepositoryAdapterIT extends PiovraIntegrationTest {

    @Autowired
    private ProductRepositoryAdapter adapter;

    @Test
    void round_trips_a_product() {
        CanonicalProduct product = CanonicalProductFixtures.simpleProduct("TEST-" + Ids.newId());

        adapter.save(product);
        Optional<CanonicalProduct> found = adapter.findBySku(product.tenantId(), product.sku());

        assertThat(found).contains(product);
    }

    @Test
    void a_second_save_updates_the_same_row_instead_of_creating_a_new_one() {
        CanonicalProduct product = CanonicalProductFixtures.simpleProduct("TEST-" + Ids.newId());
        adapter.save(product);

        CanonicalProduct repriced =
                CanonicalProductFixtures.simpleProduct(product.sku().value(), Money.euro("29.90"));
        adapter.save(repriced);

        Optional<CanonicalProduct> found = adapter.findBySku(product.tenantId(), product.sku());
        assertThat(found).map(p -> p.variants().getFirst().price()).contains(Money.euro("29.90"));
    }
}
