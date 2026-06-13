package com.talentgrid.candidate.externalCandidate.client;

import com.talentgrid.candidate.exception.BusinessException;
import com.talentgrid.candidate.externalCandidate.dto.ApplicationRequestDto;
import com.talentgrid.candidate.externalCandidate.dto.ApplicationResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class ApplicationClient {

    private final RestTemplate restTemplate;

    @Value("${application.service.url}")
    private String applicationServiceUrl;

    public ApplicationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ApplicationResponseDto createApplication(ApplicationRequestDto requestDto) {

        try {
            return restTemplate.postForObject(
                    applicationServiceUrl + "/applications",
                    requestDto,
                    ApplicationResponseDto.class
            );

        } catch (HttpClientErrorException ex) {

            HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

            if (status == HttpStatus.CONFLICT) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "Application already exists for this candidate and demand"
                );
            }

            throw new BusinessException(
                    status,
                    extractErrorMessage(ex)
            );

        } catch (RestClientException ex) {

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Automatic application submission failed because application-service is unavailable: "
                            + ex.getMessage()
            );
        }
    }

    private String extractErrorMessage(HttpClientErrorException ex) {

        String responseBody = ex.getResponseBodyAsString();

        if (responseBody != null && !responseBody.isBlank()) {
            return responseBody;
        }

        return "Application-service rejected the automatic application submission";
    }
}