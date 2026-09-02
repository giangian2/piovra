package dev.piovra.crosscutting;

import dev.piovra.crosscutting.aspect.AuditAspect;
import dev.piovra.crosscutting.aspect.ChannelCallAspect;
import dev.piovra.crosscutting.aspect.IdempotencyAspect;
import dev.piovra.crosscutting.port.AuditSink;
import dev.piovra.crosscutting.port.IdempotencyStore;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Registers the aspects when the ports they need are available.
 *
 * <p>An aspect that depends on a port only activates if someone implements it: a service that does
 * not need idempotency pays nothing and, more importantly, never discovers at runtime that the
 * annotation was doing nothing because a bean was missing.
 */
@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy
public class CrossCuttingAutoConfiguration {

    @Bean
    @ConditionalOnBean(IdempotencyStore.class)
    @ConditionalOnMissingBean
    public IdempotencyAspect idempotencyAspect(IdempotencyStore store) {
        return new IdempotencyAspect(store);
    }

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean
    public ChannelCallAspect channelCallAspect(MeterRegistry registry) {
        return new ChannelCallAspect(registry);
    }

    @Bean
    @ConditionalOnBean(AuditSink.class)
    @ConditionalOnMissingBean
    public AuditAspect auditAspect(AuditSink sink) {
        return new AuditAspect(sink);
    }
}
