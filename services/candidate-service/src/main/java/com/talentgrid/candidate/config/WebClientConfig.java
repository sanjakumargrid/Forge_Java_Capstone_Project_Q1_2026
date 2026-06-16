package com.talentgrid.candidate.config;

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
}