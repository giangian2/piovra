package dev.piovra.app.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The architectural rules of docs/10-stack-and-repo.md, made verifiable.
 *
 * <p>A rule that lives only in a document gets violated in month three without anyone noticing.
 * These tests fail the build.
 */
@AnalyzeClasses(packages = "dev.piovra", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /**
     * Rule 1: no service depends on another service. If you need data owned by another service,
     * either it arrives as an event or the boundary is wrong.
     */
    @ArchTest
    static final ArchRule services_do_not_call_each_other = noClasses()
            .that()
            .resideInAPackage("dev.piovra.publication..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "dev.piovra.catalog..",
                    "dev.piovra.inventory..",
                    "dev.piovra.order..",
                    "dev.piovra.channelconfig..",
                    "dev.piovra.feed..")
            .because("services communicate only through events: that is what makes splitting them "
                    + "into separate deployables free");

    @ArchTest
    static final ArchRule the_catalog_does_not_depend_on_other_services = noClasses()
            .that()
            .resideInAPackage("dev.piovra.catalog..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "dev.piovra.publication..",
                    "dev.piovra.inventory..",
                    "dev.piovra.order..",
                    "dev.piovra.channelconfig..",
                    "dev.piovra.feed..")
            .because("services communicate only through events");

    /**
     * Rule 2: the domain does not know the framework. That is what lets us test diffs, stock rules
     * and projections in milliseconds, without a Spring context.
     */
    @ArchTest
    static final ArchRule the_domain_does_not_know_spring = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.apache.kafka..")
            .because("the domain must stay testable without booting anything");

    /** Rule 3: the driver contract stays pure, otherwise it is not reusable outside the connectors. */
    @ArchTest
    static final ArchRule the_driver_spi_stays_pure = noClasses()
            .that()
            .resideInAPackage("dev.piovra.driver.spi..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "org.apache.kafka..", "jakarta.persistence..")
            .because("a driver must be usable from a test, a CLI or a batch job");

    /** The canonical model must not become somebody's persistence model. */
    @ArchTest
    static final ArchRule the_canonical_model_is_not_a_jpa_model = noClasses()
            .that()
            .resideInAPackage("dev.piovra.model..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..", "org.springframework..")
            .because("persistence entities are private to each service, never shared");
}
