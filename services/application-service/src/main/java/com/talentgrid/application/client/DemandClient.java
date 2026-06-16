package com.talentgrid.application.client;

import com.talentgrid.application.application.dto.DemandDto;
import com.talentgrid.application.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class DemandClient {

    private final WebClient webClient;

    public DemandClient(
            @Qualifier("demandWebClient")
            WebClient webClient
    ) {
        this.webClient = webClient;
    }

    public DemandDto getDemand(Long demandId) {

        try {
            return webClient.get()
                    .uri("/api/demands/{id}", demandId)
                    .retrieve()
                    .bodyToMono(DemandDto.class)
                    .block();

        } catch (WebClientResponseException.NotFound ex) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Demand not found with id: " + demandId
            );

        } catch (WebClientResponseException ex) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Demand-service error: " + ex.getResponseBodyAsString()
            );

        } catch (Exception ex) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to connect demand-service"
            );
        }
    }
}