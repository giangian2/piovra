package dev.piovra.catalog.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.piovra.catalog.application.port.in.FindComplianceProfileUseCase;
import dev.piovra.catalog.application.port.in.RegisterComplianceProfileUseCase;
import dev.piovra.common.TenantId;
import dev.piovra.model.compliance.ComplianceProfile;
import dev.piovra.model.compliance.ComplianceProfileType;

@RestController
@RequestMapping("/v1/compliance-profiles")
public class ComplianceProfileController {

    private final RegisterComplianceProfileUseCase registerComplianceProfileUseCase;
    private final FindComplianceProfileUseCase findComplianceProfileUseCase;

    public ComplianceProfileController(
            RegisterComplianceProfileUseCase registerComplianceProfileUseCase,
            FindComplianceProfileUseCase findComplianceProfileUseCase) {
        this.registerComplianceProfileUseCase = registerComplianceProfileUseCase;
        this.findComplianceProfileUseCase = findComplianceProfileUseCase;
    }

    @PostMapping
    public ResponseEntity<ComplianceProfile> register(
            @RequestHeader(value = "X-Piovra-Tenant", defaultValue = "default") String tenantId,
            @RequestBody ComplianceProfileRequest request) {
        ComplianceProfile saved = registerComplianceProfileUseCase.register(request.toProfile(TenantId.of(tenantId)));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}")
    public ComplianceProfile get(
            @PathVariable String id,
            @RequestHeader(value = "X-Piovra-Tenant", defaultValue = "default") String tenantId) {
        return findComplianceProfileUseCase
                .find(TenantId.of(tenantId), id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "compliance profile not found: " + id));
    }

    @GetMapping
    public List<ComplianceProfile> listByType(
            @RequestParam ComplianceProfileType type,
            @RequestHeader(value = "X-Piovra-Tenant", defaultValue = "default") String tenantId) {
        return findComplianceProfileUseCase.findByType(TenantId.of(tenantId), type);
    }
}
