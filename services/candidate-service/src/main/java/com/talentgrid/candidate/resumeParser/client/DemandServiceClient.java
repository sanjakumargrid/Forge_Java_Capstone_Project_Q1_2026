package com.talentgrid.candidate.resumeParser.client;

import com.talentgrid.candidate.resumeParser.model.DemandDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DemandServiceClient {

    // Define the base URL for the Demand Service in your application.properties
    @Value("${demand.service.url}")
    private String demandServiceBaseUrl;

    private final RestTemplate restTemplate;

    public DemandServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    public DemandDTO fetchDemandById(Long demandId) {

        String url = demandServiceBaseUrl + "/api/demands/" + demandId;

        ResponseEntity<DemandDTO> response = restTemplate.getForEntity(url, DemandDTO.class);

        return response.getBody();
    }
}