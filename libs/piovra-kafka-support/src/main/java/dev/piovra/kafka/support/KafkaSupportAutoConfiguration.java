package dev.piovra.kafka.support;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.kafka.support.mapping.Jackson2JavaTypeMapper;
import org.springframework.util.backoff.ExponentialBackOff;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.events.Topics;

/**
 * Shared Kafka plumbing: JSON (de)serialization for typed listener parameters, MDC propagation, and
 * the in-process retry-then-DLQ tier from docs/09-errors-observability.md section 2.
 *
 * <p>The 3-tier delay-topic retry (5m/30m/6h) is deliberately out of scope for this step: it needs a
 * scheduled republish mechanism that is more machinery than a first working slice justifies.
 */
@Configuration(proxyBeanMethods = false)
public class KafkaSupportAutoConfiguration {

    /**
     * Type is inferred from the {@code @KafkaListener} method's own parameter, not from a
     * {@code __TypeId__} header: producers here (the outbox relay) write plain JSON with no Jackson
     * type metadata attached.
     */
    @Bean
    @ConditionalOnMissingBean
    public RecordMessageConverter kafkaRecordMessageConverter(ObjectMapper objectMapper) {
        StringJsonMessageConverter converter = new StringJsonMessageConverter(objectMapper);
        converter.getTypeMapper().setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    @Bean
    public MdcRecordInterceptor mdcRecordInterceptor() {
        return new MdcRecordInterceptor();
    }

    /**
     * In-process retry tier: a handful of short exponential-backoff attempts, then the message moves
     * to {@code Topics.dlq(originalTopic)}. Beyond this the consumer is not blocked on a poison
     * message (docs/04-kafka-events.md section 7's golden rule).
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate, (record, exception) -> new TopicPartition(Topics.dlq(record.topic()), -1));

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(30_000L);
        return new DefaultErrorHandler(recoverer, backOff);
    }

    /**
     * Overrides Spring Boot's default {@code kafkaListenerContainerFactory} bean to wire in the JSON
     * converter, the DLQ error handler and MDC propagation, so every {@code @KafkaListener} in every
     * service gets all three without repeating configuration.
     */
    @Bean
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            RecordMessageConverter converter,
            DefaultErrorHandler errorHandler,
            MdcRecordInterceptor mdcRecordInterceptor) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, consumerFactory);
        factory.setRecordMessageConverter(converter);
        factory.setCommonErrorHandler(errorHandler);
        factory.setRecordInterceptor(mdcRecordInterceptor);
        return factory;
    }
}
