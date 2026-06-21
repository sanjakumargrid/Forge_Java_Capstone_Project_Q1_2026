package com.talentgrid.candidate.externalCandidate.client;

import com.talentgrid.candidate.exception.BusinessException;
import com.talentgrid.candidate.externalCandidate.dto.ApplicationRequestDto;
import com.talentgrid.candidate.externalCandidate.dto.ApplicationResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class ApplicationClient {

    private final WebClient applicationWebClient;

    public ApplicationClient(WebClient applicationWebClient) {
        this.applicationWebClient = applicationWebClient;
    }

    public ApplicationResponseDto createApplication(
            ApplicationRequestDto request
    ) {
        try {
            return applicationWebClient.post()
                    .uri("/api/applications")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ApplicationResponseDto.class)
                    .block();

        } catch (WebClientResponseException.Conflict ex) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Application already exists for this candidate and demand"
            );

        } catch (WebClientResponseException.BadRequest ex) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    ex.getResponseBodyAsString()
            );

        } catch (WebClientResponseException.NotFound ex) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    ex.getResponseBodyAsString()
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
}