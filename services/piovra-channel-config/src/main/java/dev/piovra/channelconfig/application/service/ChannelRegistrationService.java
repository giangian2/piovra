package dev.piovra.channelconfig.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.piovra.channelconfig.application.port.in.FindChannelUseCase;
import dev.piovra.channelconfig.application.port.in.RegisterChannelUseCase;
import dev.piovra.channelconfig.application.port.out.ChannelDefinitionRepository;
import dev.piovra.common.ChannelId;
import dev.piovra.common.TenantId;
import dev.piovra.events.ChannelConfigChanged;
import dev.piovra.model.channel.ChannelDefinition;
import dev.piovra.outbox.OutboxWriter;

/**
 * Registers channels and publishes every write on the compacted {@code channel.config.v1} topic
 * unconditionally: there is no diff here, unlike catalog. Admin operations are low-frequency and the
 * topic is compacted, so a repeated identical write costs nothing downstream, while diffing here
 * would just be complexity without a real payoff (docs/02-services.md, "channel-config").
 */
@Service
public class ChannelRegistrationService implements RegisterChannelUseCase, FindChannelUseCase {

    private final ChannelDefinitionRepository repository;
    private final OutboxWriter outboxWriter;

    public ChannelRegistrationService(ChannelDefinitionRepository repository, OutboxWriter outboxWriter) {
        this.repository = repository;
        this.outboxWriter = outboxWriter;
    }

    @Override
    @Transactional
    public ChannelDefinition register(ChannelDefinition definition) {
        ChannelDefinition saved = repository.save(definition);
        outboxWriter.append(ChannelConfigChanged.of(saved));
        return saved;
    }

    @Override
    public Optional<ChannelDefinition> find(TenantId tenantId, ChannelId channelId) {
        return repository.findByChannelId(tenantId, channelId);
    }

    @Override
    public List<ChannelDefinition> findAll(TenantId tenantId) {
        return repository.findByTenant(tenantId);
    }
}
