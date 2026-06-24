package com.talentgrid.interview.client;

import com.talentgrid.interview.exception.BusinessException;
import com.talentgrid.interview.interview.dto.ApplicationDto;
import com.talentgrid.interview.interview.dto.StageMoveRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class ApplicationClient {

    private final WebClient webClient;

    public ApplicationClient(
            @Qualifier("applicationWebClient") WebClient webClient
    ) {
        this.webClient = webClient;
    }

    public ApplicationDto getApplication(Long applicationId) {
        try {
            return webClient.get()
                    .uri("/api/v1/applications/{applicationId}", applicationId)
                    .retrieve()
                    .bodyToMono(ApplicationDto.class)
                    .block();

        } catch (WebClientResponseException.NotFound ex) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Application not found with id: " + applicationId
            );

        } catch (WebClientResponseException ex) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Application-service error: " + ex.getResponseBodyAsString()
            );

        } catch (Exception ex) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to connect application-service"
            );
        }
    }

    public ApplicationDto moveApplicationStage(
            Long applicationId,
            String targetStage,
            String reason
    ) {

        StageMoveRequest request = new StageMoveRequest();
        request.setTargetStage(targetStage);
        request.setReason(reason);

        try {
            return webClient.patch()
                    .uri("/api/v1/applications/{applicationId}/stage", applicationId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ApplicationDto.class)
                    .block();

        } catch (WebClientResponseException ex) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to move application stage: " + ex.getResponseBodyAsString()
            );

        } catch (Exception ex) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to connect application-service"
            );
        }
    }
}