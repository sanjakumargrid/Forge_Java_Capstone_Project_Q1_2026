package com.talentgrid.application.client;

import com.talentgrid.application.application.dto.JobPostingDto;
import com.talentgrid.application.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.util.List;

@Service
public class JobPostingClient {

    private final WebClient webClient;

    public JobPostingClient(
            @Qualifier("jobPostingWebClient") WebClient webClient
    ) {
        this.webClient = webClient;
    }

    public JobPostingDto getJobPosting(Long jobPostingId) {
        try {
            return webClient.get()
                    .uri("/api/job-postings/{id}", jobPostingId)
                    .retrieve()
                    .bodyToMono(JobPostingDto.class)
                    .block();

        } catch (WebClientResponseException.NotFound ex) {
            return null;

        } catch (WebClientResponseException ex) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Job-service error: " + ex.getResponseBodyAsString()
            );

        } catch (Exception ex) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to connect job-service"
            );
        }
    }

    public JobPostingDto getJobPostingByDemandId(Long demandId) {
        try {
            List<JobPostingDto> allPostings = webClient.get()
                    .uri("/api/job-postings")
                    .retrieve()
                    .bodyToFlux(JobPostingDto.class)
                    .collectList()
                    .block();

            if (allPostings == null) return null;

            return allPostings.stream()
                    .filter(jp -> demandId.equals(jp.getDemandId()))
                    .findFirst()
                    .orElse(null);
        } catch (WebClientResponseException ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Job-service error: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to connect job-service");
        }
    }
}