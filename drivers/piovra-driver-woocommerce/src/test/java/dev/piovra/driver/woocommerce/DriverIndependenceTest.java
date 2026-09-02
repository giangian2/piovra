package dev.piovra.driver.woocommerce;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * A driver knows nothing about Kafka, nothing about Spring, nothing about any service.
 *
 * <p>This is the rule that keeps drivers replaceable and testable in isolation, and that lets the
 * TCK exist. If a framework dependency ever genuinely belongs here, the responsibility almost
 * certainly belongs to the connector instead.
 */
@AnalyzeClasses(packages = "dev.piovra.driver.woocommerce", importOptions = ImportOption.DoNotIncludeTests.class)
class DriverIndependenceTest {

    @ArchTest
    static final ArchRule no_framework_dependencies = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..", "org.apache.kafka..", "jakarta.persistence..", "dev.piovra.events..")
            .because("the driver only translates: orchestration, queues and persistence belong to the connector");
}
