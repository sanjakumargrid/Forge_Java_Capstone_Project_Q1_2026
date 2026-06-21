package com.talentgrid.candidate.resumeParser.client;

import com.talentgrid.candidate.resumeParser.model.DemandDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class DemandServiceClient {

    @Value("${demand.service.url}")
    private String demandServiceBaseUrl;

    private final RestTemplate restTemplate;

    public DemandServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public DemandDTO fetchDemandById(Long demandId) {
        String url = demandServiceBaseUrl + "/api/demands/" + demandId;
        try {
            ResponseEntity<DemandDTO> response = restTemplate.getForEntity(url, DemandDTO.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Error communicating with Demand Service: " + e.getMessage());
        }
    }
}