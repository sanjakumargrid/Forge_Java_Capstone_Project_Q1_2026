package com.talentgrid.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.lang.NonNull;

/**
 * Configuration class for setting up reactive Redis interactions.
 * 
 * <p>This configuration provides a {@link ReactiveRedisTemplate} specifically tuned for
 * string-based key-value pairs, which is primarily used by the API Gateway for token
 * blacklist lookups and rate limiting state management.</p>
 */
@Configuration
public class RedisConfig {

    /**
     * Creates a {@link ReactiveRedisTemplate} configured with {@link StringRedisSerializer}
     * for both keys and values.
     *
     * @param factory the reactive Redis connection factory
     * @return a configured string-based reactive Redis template
     */
    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            @NonNull ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer serializer = new StringRedisSerializer();
        RedisSerializationContext<String, String> context = RedisSerializationContext
                .<String, String>newSerializationContext(serializer)
                .value(serializer)
                .build();
        return new ReactiveRedisTemplate<>(factory, context);
    }
}
