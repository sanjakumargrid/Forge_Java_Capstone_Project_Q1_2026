package com.talentgrid.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient candidateWebClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8081")
                .build();
    }

    @Bean
    public WebClient demandWebClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8082")
                .build();
    }
}