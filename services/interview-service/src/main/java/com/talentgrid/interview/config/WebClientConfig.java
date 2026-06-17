package com.talentgrid.interview.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient applicationWebClient(
            @Value("${application.service.url}") String applicationServiceUrl
    ) {
        return WebClient.builder()
                .baseUrl(applicationServiceUrl)
                .build();
    }

    @Bean
    public WebClient userAuthWebClient(
            @Value("${user-auth-service.url:http://localhost:8080}") String userAuthServiceUrl
    ) {
        return WebClient.builder()
                .baseUrl(userAuthServiceUrl)
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
}