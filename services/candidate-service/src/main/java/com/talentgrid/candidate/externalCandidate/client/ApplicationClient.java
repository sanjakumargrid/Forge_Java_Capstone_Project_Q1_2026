package com.talentgrid.candidate.externalCandidate.client;

import com.talentgrid.candidate.exception.BusinessException;
import com.talentgrid.candidate.externalCandidate.dto.ApplicationRequestDto;
import com.talentgrid.candidate.externalCandidate.dto.ApplicationResponseDto;
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

    public ApplicationResponseDto createApplication(ApplicationRequestDto request) {
        try {
            ApplicationResponseDto response = applicationWebClient
                    .post()
                    .uri("/api/v1/applications")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ApplicationResponseDto.class)
                    .block();

            if (response == null) {
                throw new BusinessException(
                        HttpStatus.BAD_GATEWAY,
                        "Application-service returned empty response"
                );
            }

            log.info(
                    "Automatic application submitted successfully | candidateId={} | jobPostingId={} | applicationId={}",
                    request.getCandidateId(),
                    request.getJobPostingId(),
                    response.getApplicationId()
            );

            return response;

        } catch (WebClientResponseException.Conflict ex) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Application already exists for this candidate and job posting"
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

        } catch (BusinessException ex) {
            throw ex;

        } catch (Exception ex) {
            log.error("Unable to connect application-service", ex);

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Automatic application submission failed: " + ex.getMessage()
            );
        }
    }
}