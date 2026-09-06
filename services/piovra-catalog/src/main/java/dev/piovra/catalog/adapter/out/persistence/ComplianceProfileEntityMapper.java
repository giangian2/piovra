package dev.piovra.catalog.adapter.out.persistence;

import org.springframework.stereotype.Component;

import dev.piovra.common.TenantId;
import dev.piovra.model.compliance.Address;
import dev.piovra.model.compliance.ComplianceProfile;
import dev.piovra.model.compliance.ComplianceProfileType;

@Component
public class ComplianceProfileEntityMapper {

    public ComplianceProfileEntity toEntity(ComplianceProfile profile) {
        return new ComplianceProfileEntity(
                profile.id(),
                profile.tenantId().value(),
                profile.type().name(),
                profile.name(),
                profile.address().street(),
                profile.address().city(),
                profile.address().postalCode(),
                profile.address().countryCode(),
                profile.email(),
                profile.phone(),
                profile.updatedAt());
    }

    public ComplianceProfile toDomain(ComplianceProfileEntity entity) {
        return new ComplianceProfile(
                entity.id(),
                TenantId.of(entity.tenantId()),
                ComplianceProfileType.valueOf(entity.type()),
                entity.name(),
                new Address(entity.street(), entity.city(), entity.postalCode(), entity.countryCode()),
                entity.email(),
                entity.phone(),
                entity.updatedAt());
    }
}
