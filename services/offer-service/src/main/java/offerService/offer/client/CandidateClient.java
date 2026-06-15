package offerService.offer.client;

import offerService.offer.dto.CandidateDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CandidateClient {

  private final WebClient candidateWebClient;

  public CandidateClient(
          WebClient candidateWebClient
  ) {
    this.candidateWebClient = candidateWebClient;
  }

  public CandidateDto getCandidate(
          Long candidateId
  ) {

    return candidateWebClient.get()
            .uri(
                    "/external-candidates/{candidateId}",
                    candidateId
            )
            .retrieve()
            .bodyToMono(CandidateDto.class)
            .block();
  }
}