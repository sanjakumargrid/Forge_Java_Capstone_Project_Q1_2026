package com.talentgrid.application.client;

import com.talentgrid.application.application.dto.DemandDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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

        return webClient.get()
                .uri("/api/v1/demands/{id}", demandId)
                .retrieve()
                .bodyToMono(DemandDto.class)
                .block();
    }
}