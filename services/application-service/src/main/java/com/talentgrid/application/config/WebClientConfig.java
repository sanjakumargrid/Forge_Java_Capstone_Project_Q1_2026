package com.talentgrid.application.config;

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
    public WebClient candidateWebClient(
            @Value("${candidate.service.url}") String candidateServiceUrl,
            JwtTokenService jwtTokenService
    ) {
        return WebClient.builder()
                .baseUrl(candidateServiceUrl)
                .filter(authHeaderFilter(jwtTokenService))
                .build();
    }

    @Bean(name = "jobPostingWebClient")
    public WebClient jobPostingWebClient(
            @Value("${job.service.url}") String jobServiceUrl,
            JwtTokenService jwtTokenService
    ) {
        return WebClient.builder()
                .baseUrl(jobServiceUrl)
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
}