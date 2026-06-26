package com.talentgrid.application.config;

import com.talentgrid.shared.auth.jwt.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean(name = "candidateWebClient")
    public WebClient candidateWebClient(
            @Value("${candidate.service.url}") String candidateServiceUrl,
            JwtTokenService jwtTokenService
    ) {
        return WebClient.builder()
                .baseUrl(candidateServiceUrl)
                .filter((request, next) -> {
                    String internalToken = jwtTokenService.generateInternalServiceToken();

                    ClientRequest clientRequest = ClientRequest.from(request)
                            .headers(headers -> headers.setBearerAuth(internalToken))
                            .build();

                    return next.exchange(clientRequest);
                })
                .build();
    }

    @Bean(name = "jobPostingWebClient")
    public WebClient jobPostingWebClient(
            @Value("${job.service.url}") String jobServiceUrl,
            JwtTokenService jwtTokenService
    ) {
        return WebClient.builder()
                .baseUrl(jobServiceUrl)
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