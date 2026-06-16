package com.talentgrid.application.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient candidateWebClient(
            @Value("${candidate.service.url}") String candidateServiceUrl
    ) {
        return WebClient.builder()
                .baseUrl(candidateServiceUrl)
                .build();
    }

    @Bean
    public WebClient demandWebClient(
            @Value("${demand.service.url}") String demandServiceUrl
    ) {
        return WebClient.builder()
                .baseUrl(demandServiceUrl)
                .build();
    }
}