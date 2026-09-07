package dev.piovra.publication.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.piovra.common.ChannelId;
import dev.piovra.common.Sku;
import dev.piovra.common.TenantId;
import dev.piovra.publication.application.port.in.FindChannelListingUseCase;
import dev.piovra.publication.domain.ChannelListing;

/** Read-only publication status, for other apps/CRM to poll: no Kafka integration required on their
 * side, same {@code X-Piovra-Tenant} header convention as every other REST endpoint in this repo. */
@RestController
@RequestMapping("/v1/listings")
public class ChannelListingController {

    private final FindChannelListingUseCase findChannelListingUseCase;

    public ChannelListingController(FindChannelListingUseCase findChannelListingUseCase) {
        this.findChannelListingUseCase = findChannelListingUseCase;
    }

    @GetMapping("/{sku}")
    public List<ChannelListing> forSku(
            @PathVariable String sku,
            @RequestHeader(value = "X-Piovra-Tenant", defaultValue = "default") String tenantId) {
        return findChannelListingUseCase.findAllForSku(TenantId.of(tenantId), Sku.of(sku));
    }

    @GetMapping("/{sku}/{channelId}")
    public ChannelListing forSkuAndChannel(
            @PathVariable String sku,
            @PathVariable String channelId,
            @RequestHeader(value = "X-Piovra-Tenant", defaultValue = "default") String tenantId) {
        return findChannelListingUseCase
                .find(TenantId.of(tenantId), Sku.of(sku), ChannelId.of(channelId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "listing not found: " + sku + "/" + channelId));
    }
}
