package com.talentgrid.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot auto-configuration for {@code talentgrid-kafka}.
 *
 * <p>This class is referenced in
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * so it activates automatically when this JAR is on the classpath — no
 * {@code @Import} or {@code @EnableKafka} needed in consuming services.</p>
 *
 * <p>What gets auto-configured:
 * <ul>
 *   <li>A shared {@link ObjectMapper} with {@link JavaTimeModule} registered
 *       (so {@code LocalDateTime} fields in {@code BaseEvent} serialize correctly).</li>
 *   <li>All Kafka topic beans via {@link TalentGridTopicConfig}.</li>
 * </ul>
 * </p>
 */
@AutoConfiguration
@Import(TalentGridTopicConfig.class)
public class KafkaAutoConfiguration {

    /**
     * Provides a shared {@link ObjectMapper} with Java 8 time support.
     * Uses {@code @ConditionalOnMissingBean} so a service can override it
     * if it needs custom configuration.
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper talentGridObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
