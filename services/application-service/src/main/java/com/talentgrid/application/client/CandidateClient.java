package com.talentgrid.application.client;

import com.talentgrid.application.application.dto.candidate.ExternalCandidateDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class CandidateClient {

    private final WebClient candidateWebClient;

    public CandidateClient(
            @Qualifier("candidateWebClient") WebClient candidateWebClient
    ) {
        this.candidateWebClient = candidateWebClient;
    }

    public ExternalCandidateDto getCandidateById(Long candidateId) {
        try {
            return candidateWebClient
                    .get()
                    .uri("/api/v1/external-candidates/internal/{candidateId}", candidateId)
                    .retrieve()
                    .bodyToMono(ExternalCandidateDto.class)
                    .block();

        } catch (WebClientResponseException ex) {
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