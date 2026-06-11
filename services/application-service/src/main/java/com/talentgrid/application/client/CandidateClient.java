package com.talentgrid.application.client;

import com.talentgrid.application.application.dto.candidate.ExternalCandidateDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class CandidateClient {

  private final WebClient webClient;

  public CandidateClient(
          @Qualifier("candidateWebClient")
          WebClient webClient
  ) {
    this.webClient = webClient;
  }

  public ExternalCandidateDto getCandidate(Long candidateId) {

    return webClient.get()
            .uri("/api/v1/external-candidates/{id}",
                    candidateId)
            .retrieve()
            .bodyToMono(ExternalCandidateDto.class)
            .block();
  }
}