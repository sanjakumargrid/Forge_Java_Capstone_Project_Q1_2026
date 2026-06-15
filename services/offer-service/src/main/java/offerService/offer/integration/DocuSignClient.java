package offerService.offer.integration;

import lombok.extern.slf4j.Slf4j;
import offerService.offer.entity.Offer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class DocuSignClient {

    public String createEnvelope(Offer offer) {
        // TODO: Replace with real DocuSign API integration
        // Requires: DocuSign JWT OAuth, envelope creation via eSignature REST API
        log.warn("[DocuSignClient] createEnvelope — real DocuSign API not yet integrated. Returning stub envelope ID. offerId={}", offer.getId());

        String envelopeId = "stub-envelope-" + offer.getId() + "-" + UUID.randomUUID();
        log.info("[DocuSignClient] Stub envelope created | envelopeId={}", envelopeId);

        return envelopeId;
    }

    public void cancelEnvelope(String envelopeId) {
        // TODO: Replace with real DocuSign VOID envelope API call
        log.warn("[DocuSignClient] cancelEnvelope — real DocuSign API not yet integrated. envelopeId={}", envelopeId);
    }
}
