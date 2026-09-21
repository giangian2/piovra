package dev.piovra.testsupport;

import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Base class for a service's Postgres+Kafka integration tests: repositories, outbox relays,
 * consumers (docs/12-development-guidelines.md section 6, "Integration" row).
 */
@SpringBootTest
public abstract class PiovraIntegrationTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        PiovraPostgresContainer.registerProperties(registry);
        PiovraKafkaContainer.registerProperties(registry);
    }

    /**
     * Runs an outbound port that needs a caller-owned transaction - anything taking a pessimistic
     * lock, such as {@code StockLevelRepository.lockOrCreate} - the way an application service does
     * in production. Not {@code @Transactional} on the test class: that rolls back at the end, and
     * these tests have to see their own writes from a Kafka listener thread.
     */
    protected final <T> T inTransaction(Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }

    protected final void inTransaction(Runnable work) {
        transactionTemplate.executeWithoutResult(status -> work.run());
    }
}
