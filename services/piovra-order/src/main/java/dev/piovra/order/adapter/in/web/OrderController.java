package dev.piovra.order.adapter.in.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.piovra.common.TenantId;
import dev.piovra.model.order.CanonicalOrder;
import dev.piovra.order.application.port.in.FindOrderUseCase;
import dev.piovra.order.application.port.in.IngestOrderUseCase;

/** Manual/ops ingestion path, standing in for a real driver poller/webhook until one exists (same
 * role the REST batch endpoint has for stock in piovra-inventory). */
@RestController
@RequestMapping("/v1/orders")
public class OrderController {

    private final IngestOrderUseCase ingestOrderUseCase;
    private final FindOrderUseCase findOrderUseCase;

    public OrderController(IngestOrderUseCase ingestOrderUseCase, FindOrderUseCase findOrderUseCase) {
        this.ingestOrderUseCase = ingestOrderUseCase;
        this.findOrderUseCase = findOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<CanonicalOrder> ingest(
            @RequestHeader(value = "X-Piovra-Tenant", defaultValue = "default") String tenantId,
            @Valid @RequestBody OrderIngestRequest request) {
        TenantId tenant = TenantId.of(tenantId);
        CanonicalOrder order = ingestOrderUseCase.ingest(request.toCanonicalOrder(tenant), null);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(order);
    }

    @GetMapping("/{orderId}")
    public CanonicalOrder get(
            @PathVariable String orderId,
            @RequestHeader(value = "X-Piovra-Tenant", defaultValue = "default") String tenantId) {
        return findOrderUseCase
                .find(TenantId.of(tenantId), orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found: " + orderId));
    }
}
