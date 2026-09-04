package dev.piovra.channelconfig.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.piovra.channelconfig.application.port.in.FindChannelUseCase;
import dev.piovra.channelconfig.application.port.in.RegisterChannelUseCase;
import dev.piovra.common.ChannelId;
import dev.piovra.common.TenantId;
import dev.piovra.model.channel.ChannelDefinition;

@RestController
@RequestMapping("/v1/channels")
public class ChannelDefinitionController {

    private final RegisterChannelUseCase registerChannelUseCase;
    private final FindChannelUseCase findChannelUseCase;

    public ChannelDefinitionController(
            RegisterChannelUseCase registerChannelUseCase, FindChannelUseCase findChannelUseCase) {
        this.registerChannelUseCase = registerChannelUseCase;
        this.findChannelUseCase = findChannelUseCase;
    }

    @PutMapping("/{channelId}")
    public ChannelDefinition register(
            @PathVariable String channelId,
            @RequestHeader(value = "X-Piovra-Tenant", defaultValue = "default") String tenantId,
            @RequestBody ChannelDefinitionRequest request) {
        return registerChannelUseCase.register(request.toDefinition(TenantId.of(tenantId), ChannelId.of(channelId)));
    }

    @GetMapping("/{channelId}")
    public ChannelDefinition get(
            @PathVariable String channelId,
            @RequestHeader(value = "X-Piovra-Tenant", defaultValue = "default") String tenantId) {
        return findChannelUseCase
                .find(TenantId.of(tenantId), ChannelId.of(channelId))
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "channel not found: " + channelId));
    }

    @GetMapping
    public List<ChannelDefinition> list(
            @RequestHeader(value = "X-Piovra-Tenant", defaultValue = "default") String tenantId) {
        return findChannelUseCase.findAll(TenantId.of(tenantId));
    }
}
