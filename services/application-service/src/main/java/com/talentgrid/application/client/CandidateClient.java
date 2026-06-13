package com.talentgrid.application.client;

import com.talentgrid.application.application.dto.candidate.ExternalCandidateDto;
import com.talentgrid.application.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class CandidateClient {

    private final WebClient webClient;

    public CandidateClient(
            @Qualifier("candidateWebClient")
            WebClient webClient
    ) {
        this.webClient = webClient;
    }

    public ExternalCandidateDto getCandidate(Long candidateId) {

        try {
            return webClient.get()
                    .uri("/api/v1/external-candidates/{id}", candidateId)
                    .retrieve()
                    .bodyToMono(ExternalCandidateDto.class)
                    .block();

        } catch (WebClientResponseException.NotFound ex) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Candidate not found with id: " + candidateId
            );

        } catch (WebClientResponseException ex) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Candidate-service error: " + ex.getResponseBodyAsString()
            );

        } catch (Exception ex) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to connect candidate-service"
            );
        }
    }
}