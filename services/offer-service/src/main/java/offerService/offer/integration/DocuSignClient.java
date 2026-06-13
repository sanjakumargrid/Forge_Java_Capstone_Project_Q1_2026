package offerService.offer.integration;

import offerService.offer.entity.Offer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DocuSignClient {

    public String createEnvelope(Offer offer) {
        return "mock-envelope-" + offer.getId() + "-" + UUID.randomUUID();
    }

    public void cancelEnvelope(String envelopeId) {
        // Mock cancellation for now.
    }
}
