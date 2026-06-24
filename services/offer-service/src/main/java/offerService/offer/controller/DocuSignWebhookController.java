package offerService.offer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import offerService.offer.dto.DocuSignWebhookDto;
import offerService.offer.service.OfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks/docusign")
@RequiredArgsConstructor
public class DocuSignWebhookController {

    private final OfferService offerService;

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody JsonNode payload) {

        String envelopeId = payload.path("data").path("envelopeId").asText(null);
        String status = payload.path("data").path("envelopeSummary").path("status").asText(null);
        
        if (envelopeId == null) {
            envelopeId = payload.path("envelopeId").asText(null);
        }
        if (status == null) {
            status = payload.path("status").asText(null);
        }

        if (envelopeId == null || status == null) {
            return ResponseEntity.badRequest().build();
        }

        DocuSignWebhookDto dto = new DocuSignWebhookDto();
        dto.setEnvelopeId(envelopeId);
        dto.setStatus(status);

        offerService.handleDocuSignWebhook(dto);

        return ResponseEntity.ok().build();
    }
}
