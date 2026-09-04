package dev.piovra.channelconfig.adapter.out.kafka;

import dev.piovra.outbox.OutboxRepository;

public interface ChannelConfigOutboxRepository extends OutboxRepository<ChannelConfigOutboxEvent> {}
