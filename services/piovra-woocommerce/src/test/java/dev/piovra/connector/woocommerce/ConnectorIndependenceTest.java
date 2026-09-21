package dev.piovra.connector.woocommerce;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The connector is a service like any other: it talks to the rest of the system only through events.
 *
 * <p>{@code ArchitectureTest} in piovra-core cannot cover this package - the connector is not on
 * that deployable's classpath - so the rule lives with the classes it constrains, the same way
 * {@code DriverIndependenceTest} does in each driver module.
 */
@AnalyzeClasses(packages = "dev.piovra.connector", importOptions = ImportOption.DoNotIncludeTests.class)
class ConnectorIndependenceTest {

    @ArchTest
    static final ArchRule the_connector_does_not_depend_on_other_services = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "dev.piovra.catalog..",
                    "dev.piovra.inventory..",
                    "dev.piovra.order..",
                    "dev.piovra.publication..",
                    "dev.piovra.channelconfig..",
                    "dev.piovra.feed..")
            .because("a connector reaches the rest of the system through events, like every service: "
                    + "that is what keeps it a separate deployable");

    /** The polling window is arithmetic on two instants. It has no business knowing about Spring. */
    @ArchTest
    static final ArchRule the_domain_does_not_know_spring = noClasses()
            .that()
            .resideInAPackage("..connector..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.apache.kafka..")
            .because("the domain must stay testable without booting anything");
}
