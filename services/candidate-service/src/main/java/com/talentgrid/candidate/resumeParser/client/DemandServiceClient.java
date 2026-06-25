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

    public DemandDTO fetchDemandById(Long jobPostingId) {
        String url = demandServiceBaseUrl + "/api/v1/demands/" + jobPostingId;
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            org.springframework.web.context.request.RequestAttributes requestAttributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof org.springframework.web.context.request.ServletRequestAttributes attributes) {
                jakarta.servlet.http.HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    headers.set("Authorization", authHeader);
                }
            }
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            ResponseEntity<DemandDTO> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, DemandDTO.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Error communicating with Demand Service: " + e.getMessage());
        }
    }
}