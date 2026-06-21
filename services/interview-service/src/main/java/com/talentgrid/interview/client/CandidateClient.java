package com.talentgrid.interview.client;

import com.talentgrid.interview.client.dto.CandidateDto;
import com.talentgrid.interview.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
public class CandidateClient {

    private final WebClient webClient;

    public CandidateClient(
            @Qualifier("candidateWebClient") WebClient webClient
    ) {
        this.webClient = webClient;
    }

    public CandidateDto getCandidate(Long candidateId) {
        try {
            return webClient.get()
                    .uri("/api/v1/external-candidates/{id}", candidateId)
                    .retrieve()
                    .bodyToMono(CandidateDto.class)
                    .block();

        } catch (WebClientResponseException.NotFound ex) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Candidate not found with id: " + candidateId
            );

        } catch (Exception ex) {
            log.error("[CandidateClient] Unable to connect to candidate-service for candidateId={}: {}",
                    candidateId, ex.getMessage());
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to connect to candidate-service"
            );
        }
    }
}
