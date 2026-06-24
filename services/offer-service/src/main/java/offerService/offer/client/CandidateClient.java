package offerService.offer.client;

import lombok.extern.slf4j.Slf4j;
import offerService.offer.dto.CandidateDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
public class CandidateClient {

    private final WebClient candidateWebClient;

    public CandidateClient(
            @Qualifier("candidateWebClient") WebClient candidateWebClient
    ) {
        this.candidateWebClient = candidateWebClient;
    }

    public CandidateDto getCandidate(Long candidateId) {

        try {
            return candidateWebClient.get()
                    .uri("/api/v1/external-candidates/{candidateId}", candidateId)
                    .retrieve()
                    .bodyToMono(CandidateDto.class)
                    .block();

        } catch (WebClientResponseException.NotFound ex) {
            log.error("Candidate not found with id {}", candidateId, ex);

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Candidate not found with id: " + candidateId
            );

        } catch (WebClientResponseException ex) {
            log.error(
                    "Candidate-service returned error for candidate id {}. Status: {}, Body: {}",
                    candidateId,
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString(),
                    ex
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Candidate-service error while fetching candidate details"
            );

        } catch (Exception ex) {
            log.error("Unable to connect candidate-service for candidate id {}", candidateId, ex);

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to connect candidate-service"
            );
        }
    }
}