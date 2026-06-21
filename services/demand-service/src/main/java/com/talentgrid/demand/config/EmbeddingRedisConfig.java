package com.talentgrid.demand.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class EmbeddingRedisConfig {

    @Bean
    public RedisTemplate<String, float[]> embeddingRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        
        RedisTemplate<String, float[]> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(float[].class));
        
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(float[].class));
        
        template.afterPropertiesSet();
        return template;
    }
}
