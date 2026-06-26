package com.talentgrid.candidate.config;

import com.talentgrid.shared.auth.jwt.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient applicationWebClient(
            @Value("${application.service.url}") String applicationServiceUrl,
            JwtTokenService jwtTokenService
    ) {
        return WebClient.builder()
                .baseUrl(applicationServiceUrl)
                .filter((request, next) -> {
                    String internalToken = jwtTokenService.generateInternalServiceToken();

                    ClientRequest clientRequest = ClientRequest.from(request)
                            .headers(headers -> headers.setBearerAuth(internalToken))
                            .build();

                    return next.exchange(clientRequest);
                })
                .build();
    }
}