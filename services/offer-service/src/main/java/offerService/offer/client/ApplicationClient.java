package offerService.offer.client;

import offerService.offer.dto.ApplicationDto;
import offerService.offer.dto.StageMoveRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class ApplicationClient {

    private final WebClient applicationWebClient;

    public ApplicationClient(WebClient applicationWebClient) {
        this.applicationWebClient = applicationWebClient;
    }

    public ApplicationDto getApplication(Long applicationId) {

        try {

            return applicationWebClient.get()
                    .uri("/api/v1/applications/{applicationId}", applicationId)
                    .retrieve()
                    .bodyToMono(ApplicationDto.class)
                    .block();

        } catch (WebClientResponseException.NotFound ex) {
            throw new RuntimeException(
                    "Application not found with id: " + applicationId
            );

        } catch (WebClientResponseException ex) {
            throw new RuntimeException(
                    "Application-service error: "
                            + ex.getStatusCode()
                            + " - "
                            + ex.getResponseBodyAsString()
            );

        } catch (Exception ex) {
            throw new RuntimeException(
                    "Unable to connect application-service",
                    ex
            );
        }
    }

    public ApplicationDto moveApplicationStage(
            Long applicationId,
            String targetStage,
            String reason
    ) {

        StageMoveRequest request = new StageMoveRequest();
        request.setTargetStage(targetStage);
        request.setReason(reason);

        try {

            return applicationWebClient.patch()
                    .uri("/api/v1/applications/{applicationId}/stage", applicationId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ApplicationDto.class)
                    .block();

        } catch (WebClientResponseException ex) {
            throw new RuntimeException(
                    "Unable to move application stage. Status: "
                            + ex.getStatusCode()
                            + ", response: "
                            + ex.getResponseBodyAsString()
            );

        } catch (Exception ex) {
            throw new RuntimeException(
                    "Unable to connect application-service",
                    ex
            );
        }
    }
}
