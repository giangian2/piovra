package dev.piovra.outbox;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Scheduled} for every app that pulls in an outbox-writing module, so
 * {@link OutboxRelay} beans run without each app remembering to declare {@code @EnableScheduling}
 * itself (docs/12-development-guidelines.md section 1 - "single point").
 *
 * <p>With {@code spring.threads.virtual.enabled=true} (already set in every app's
 * {@code application.yml}), Spring Boot's default {@code TaskScheduler} is virtual-thread backed, so
 * this needs no extra pool configuration (docs/12-development-guidelines.md section 5.3).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class OutboxAutoConfiguration {}
