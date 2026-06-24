//package com.talentgrid.application.client;
//
//import com.talentgrid.application.application.dto.candidate.ExternalCandidateDto;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestClient;
//import org.springframework.web.client.RestClientResponseException;
//import org.springframework.http.client.ClientHttpRequestInterceptor;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//import jakarta.servlet.http.HttpServletRequest;
//import com.talentgrid.shared.auth.jwt.JwtTokenService;
//
//@Component
//@RequiredArgsConstructor
//public class CandidateClient {
//
//    private final RestClient.Builder restClientBuilder;
//    private final JwtTokenService jwtTokenService;
//
//    @Value("${candidate.service.url}")
//    private String candidateServiceUrl;
//
//    public ExternalCandidateDto getCandidateById(Long candidateId) {
//
//        try {
//            return restClientBuilder
//                    .baseUrl(candidateServiceUrl)
//                    .requestInterceptor(authHeaderInterceptor())
//                    .build()
//                    .get()
//                    .uri("/api/v1/external-candidates/{candidateId}", candidateId)
//                    .retrieve()
//                    .body(ExternalCandidateDto.class);
//
//        } catch (RestClientResponseException e) {
//            throw new IllegalStateException(
//                    "Failed to fetch candidate from candidate-service. Candidate id: "
//                            + candidateId
//                            + ", status: "
//                            + e.getStatusCode()
//                            + ", response: "
//                            + e.getResponseBodyAsString(),
//                    e
//            );
//        }
//    }
//
//    private ClientHttpRequestInterceptor authHeaderInterceptor() {
//        return (request, body, execution) -> {
//            String token = null;
//            var attrs = RequestContextHolder.getRequestAttributes();
//            if (attrs instanceof ServletRequestAttributes servletAttrs) {
//                HttpServletRequest httpServletRequest = servletAttrs.getRequest();
//                String authHeader = httpServletRequest.getHeader("Authorization");
//                if (authHeader != null && authHeader.startsWith("Bearer ")) {
//                    token = authHeader.substring(7);
//                }
//            }
//
//            if (token == null) {
//                token = jwtTokenService.generateInternalServiceToken();
//            }
//
//            request.getHeaders().setBearerAuth(token);
//            return execution.execute(request, body);
//        };
//    }
//}

package com.talentgrid.application.client;

import com.talentgrid.application.application.dto.candidate.ExternalCandidateDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CandidateClient {

    private final RestClient restClient;

    public CandidateClient(
            RestClient.Builder restClientBuilder,
            @Value("${candidate.service.url:http://localhost:8082}") String candidateServiceUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(candidateServiceUrl)
                .build();
    }

    public ExternalCandidateDto getCandidateById(Long candidateId) {
        try {
            return restClient
                    .get()
                    .uri("/api/v1/external-candidates/internal/{candidateId}", candidateId)
                    .retrieve()
                    .body(ExternalCandidateDto.class);

        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Failed to fetch candidate from candidate-service. Candidate id: "
                            + candidateId
                            + ", status: "
                            + ex.getStatusCode()
                            + ", response: "
                            + ex.getResponseBodyAsString(),
                    ex
            );
        }
    }
}