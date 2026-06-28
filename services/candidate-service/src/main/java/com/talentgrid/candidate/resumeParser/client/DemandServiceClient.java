package com.talentgrid.candidate.resumeParser.client;

import com.talentgrid.candidate.resumeParser.model.DemandDTO;
import com.talentgrid.shared.auth.jwt.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class DemandServiceClient {

    @Value("${demand.service.url}")
    private String demandServiceBaseUrl;

    private final RestTemplate restTemplate;
    private final JwtTokenService jwtTokenService;

    public DemandServiceClient(RestTemplate restTemplate,
                               JwtTokenService jwtTokenService) {
        this.restTemplate = restTemplate;
        this.jwtTokenService = jwtTokenService;
    }

    public DemandDTO fetchDemandById(Long jobPostingId) {
        String url = demandServiceBaseUrl + "/api/v1/demands/" + jobPostingId;

        try {
            HttpHeaders headers = new HttpHeaders();

            // Important:
            // Do not forward external candidate token to demand-service.
            // External candidate does not have DEMAND_VIEW.
            // So candidate-service should call demand-service using internal service JWT.
            String internalToken = jwtTokenService.generateInternalServiceToken();
            headers.setBearerAuth(internalToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<DemandDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    DemandDTO.class
            );

            return response.getBody();

        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (HttpClientErrorException e) {
            throw new RuntimeException(
                    "Error communicating with Demand Service: "
                            + e.getStatusCode()
                            + " - "
                            + e.getResponseBodyAsString()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error communicating with Demand Service: " + e.getMessage());
        }
    }
}