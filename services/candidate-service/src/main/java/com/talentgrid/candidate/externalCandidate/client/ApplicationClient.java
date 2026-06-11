package com.talentgrid.candidate.externalCandidate.client;

import com.talentgrid.candidate.externalCandidate.dto.ApplicationRequestDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ApplicationClient {

    private final RestTemplate restTemplate;

    @Value("${application.service.url}")
    private String applicationServiceUrl;

    public ApplicationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void createApplication(ApplicationRequestDto requestDto) {

        restTemplate.postForObject(
                applicationServiceUrl + "/applications",
                requestDto,
                Object.class
        );
    }
}