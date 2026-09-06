package dev.piovra.catalog.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.piovra.catalog.adapter.out.persistence.ComplianceProfileRepositoryAdapter;
import dev.piovra.common.Ids;
import dev.piovra.common.TenantId;
import dev.piovra.model.compliance.Address;
import dev.piovra.model.compliance.ComplianceProfile;
import dev.piovra.model.compliance.ComplianceProfileType;
import dev.piovra.testsupport.PiovraIntegrationTest;

class ComplianceProfileRepositoryAdapterIT extends PiovraIntegrationTest {

    private static final TenantId TENANT = TenantId.of("acme");

    @Autowired
    private ComplianceProfileRepositoryAdapter adapter;

    @Test
    void round_trips_a_profile() {
        ComplianceProfile profile = manufacturer();

        adapter.save(profile);
        Optional<ComplianceProfile> found = adapter.findById(TENANT, profile.id());

        assertThat(found).contains(profile);
    }

    @Test
    void finds_profiles_by_type() {
        ComplianceProfile profile = manufacturer();
        adapter.save(profile);

        assertThat(adapter.findByTenantAndType(TENANT, ComplianceProfileType.MANUFACTURER))
                .extracting(ComplianceProfile::id)
                .contains(profile.id());
    }

    private static ComplianceProfile manufacturer() {
        return new ComplianceProfile(
                Ids.newId(),
                TENANT,
                ComplianceProfileType.MANUFACTURER,
                "Acme Srl",
                new Address("Via Roma 1", "Milano", "20100", "IT"),
                "compliance@acme.test",
                null,
                Instant.now());
    }
}
