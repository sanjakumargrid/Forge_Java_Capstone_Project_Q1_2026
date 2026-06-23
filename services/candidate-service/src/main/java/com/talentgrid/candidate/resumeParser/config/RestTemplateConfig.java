package com.talentgrid.candidate.resumeParser.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory; // <-- The native Java client
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Uses Java's native HTTP client, supports PATCH, and requires zero extra dependencies!
        return new RestTemplate(new JdkClientHttpRequestFactory());
    }
}