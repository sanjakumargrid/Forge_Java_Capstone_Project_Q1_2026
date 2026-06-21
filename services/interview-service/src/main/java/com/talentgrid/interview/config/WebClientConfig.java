package com.talentgrid.interview.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import com.talentgrid.shared.auth.jwt.JwtTokenService;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient applicationWebClient(
            @Value("${application.service.url}") String applicationServiceUrl,
            JwtTokenService jwtTokenService
    ) {
        return WebClient.builder()
                .baseUrl(applicationServiceUrl)
                .filter(authHeaderFilter(jwtTokenService))
                .build();
    }

    @Bean
    public WebClient userAuthWebClient(
            @Value("${user-auth-service.url:http://localhost:8080}") String userAuthServiceUrl,
            JwtTokenService jwtTokenService
    ) {
        return WebClient.builder()
                .baseUrl(userAuthServiceUrl)
                .filter(authHeaderFilter(jwtTokenService))
                .build();
    }

    @Bean
    public WebClient candidateWebClient(
            @Value("${candidate.service.url:http://localhost:8084}") String candidateServiceUrl,
            JwtTokenService jwtTokenService
    ) {
        return WebClient.builder()
                .baseUrl(candidateServiceUrl)
                .filter(authHeaderFilter(jwtTokenService))
                .build();
    }

    private ExchangeFilterFunction authHeaderFilter(JwtTokenService jwtTokenService) {
        return (request, next) -> {
            String token = null;
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes servletAttrs) {
                HttpServletRequest httpServletRequest = servletAttrs.getRequest();
                String authHeader = httpServletRequest.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            if (token == null) {
                token = jwtTokenService.generateInternalServiceToken();
            }

            String finalToken = token;
            var clientRequest = org.springframework.web.reactive.function.client.ClientRequest.from(request)
                    .headers(headers -> headers.setBearerAuth(finalToken))
                    .build();
            return next.exchange(clientRequest);
        };
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
}