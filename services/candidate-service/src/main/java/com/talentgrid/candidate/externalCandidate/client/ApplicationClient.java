package com.talentgrid.candidate.externalCandidate.client;

import com.talentgrid.candidate.exception.BusinessException;
import com.talentgrid.candidate.externalCandidate.dto.ApplicationRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
public class ApplicationClient {

    private final WebClient applicationWebClient;

    public ApplicationClient(WebClient applicationWebClient) {
        this.applicationWebClient = applicationWebClient;
    }

    public void createApplication(ApplicationRequestDto request) {
        try {
            applicationWebClient
                    .post()
                    .uri("/api/v1/applications")
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Automatic application submitted successfully | candidateId={} | demandId={}",
                    request.getCandidateId(),
                    request.getJobPostingId()
            );

        } catch (WebClientResponseException.Conflict ex) {
            log.warn("Application already exists for candidateId={} and demandId={}. Skipping.",
                    request.getCandidateId(),
                    request.getJobPostingId()
            );

        } catch (WebClientResponseException ex) {
            log.error("Application-service error | status={} | body={}",
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString()
            );

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Application-service error: "
                            + ex.getStatusCode()
                            + " - "
                            + ex.getResponseBodyAsString()
            );

        } catch (Exception ex) {
            log.error("Unable to connect application-service", ex);

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Automatic application submission failed: " + ex.getMessage()
            );
        }
    }
}