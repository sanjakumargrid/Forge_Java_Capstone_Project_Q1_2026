package com.talentgrid.application.client;

import com.talentgrid.application.application.dto.candidate.ExternalCandidateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class CandidateClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${candidate.service.url}")
    private String candidateServiceUrl;

    public ExternalCandidateDto getCandidateById(Long candidateId) {

        try {
            return restClientBuilder
                    .baseUrl(candidateServiceUrl)
                    .build()
                    .get()
                    .uri("/candidates/{candidateId}", candidateId)
                    .retrieve()
                    .body(ExternalCandidateDto.class);

        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Failed to fetch candidate from candidate-service. Candidate id: "
                            + candidateId
                            + ", status: "
                            + e.getStatusCode()
                            + ", response: "
                            + e.getResponseBodyAsString(),
                    e
            );
        }
    }
}