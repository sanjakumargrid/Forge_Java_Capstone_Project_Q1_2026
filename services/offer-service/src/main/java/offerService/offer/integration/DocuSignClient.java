package offerService.offer.integration;

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.auth.OAuth;
import com.docusign.esign.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import offerService.offer.dto.CandidateDto;
import offerService.offer.entity.Offer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocuSignClient {

    private final DocuSignProperties properties;

    public String createEnvelope(
            Offer offer,
            CandidateDto candidate,
            byte[] pdfBytes
    ) {

        try {

            ApiClient apiClient = createAuthenticatedClient();

            EnvelopeDefinition envelope =
                    buildEnvelope(
                            offer,
                            candidate,
                            pdfBytes
                    );

            EnvelopesApi envelopesApi =
                    new EnvelopesApi(apiClient);

            EnvelopeSummary summary =
                    envelopesApi.createEnvelope(
                            properties.getAccountId(),
                            envelope
                    );

            log.info(
                    "DocuSign envelope created successfully. envelopeId={}",
                    summary.getEnvelopeId()
            );

            return summary.getEnvelopeId();

        } catch (Exception e) {

            log.error(
                    "Failed to create DocuSign envelope",
                    e
            );

            throw new RuntimeException(
                    "Unable to create DocuSign envelope",
                    e
            );
        }
    }

    public void cancelEnvelope(
            String envelopeId
    ) {

        try {

            ApiClient apiClient =
                    createAuthenticatedClient();

            EnvelopesApi envelopesApi =
                    new EnvelopesApi(apiClient);

            Envelope envelope =
                    new Envelope();

            envelope.setStatus("voided");
            envelope.setVoidedReason(
                    "Offer cancelled by recruiter"
            );

            envelopesApi.update(
                    properties.getAccountId(),
                    envelopeId,
                    envelope
            );

            log.info(
                    "Envelope voided successfully. envelopeId={}",
                    envelopeId
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to void envelope",
                    e
            );
        }
    }

    private ApiClient createAuthenticatedClient()
            throws Exception {

        ApiClient apiClient =
                new ApiClient(
                        properties.getBasePath()
                );

        byte[] privateKey =
                Files.readAllBytes(
                        new ClassPathResource(
                                properties.getPrivateKeyPath()
                        ).getFile().toPath()
                );

        List<String> scopes =
                List.of(
                        OAuth.Scope_SIGNATURE,
                        OAuth.Scope_IMPERSONATION
                );

        OAuth.OAuthToken token =
                apiClient.requestJWTUserToken(
                        properties.getIntegrationKey(),
                        properties.getUserId(),
                        scopes,
                        privateKey,
                        3600
                );

        apiClient.setAccessToken(
                token.getAccessToken(),
                token.getExpiresIn()
        );

        return apiClient;
    }

    private EnvelopeDefinition buildEnvelope(
            Offer offer,
            CandidateDto candidate,
            byte[] pdfBytes
    ) {

        String documentBase64 =
                Base64.getEncoder()
                        .encodeToString(pdfBytes);

        Document document =
                new Document();

        document.setDocumentBase64(
                documentBase64
        );

        document.setName(
                "OfferLetter.pdf"
        );

        document.setFileExtension(
                "pdf"
        );

        document.setDocumentId(
                "1"
        );

        SignHere signHere =
                new SignHere();

        signHere.setDocumentId(
                "1"
        );

        signHere.setPageNumber(
                "1"
        );

        signHere.setXPosition(
                "100"
        );

        signHere.setYPosition(
                "650"
        );

        Tabs tabs =
                new Tabs();

        tabs.setSignHereTabs(
                Collections.singletonList(
                        signHere
                )
        );

        Signer signer =
                new Signer();

        signer.setEmail(
                candidate.getEmail()
        );

        signer.setName(
                candidate.getFirstName()
                        + " "
                        + candidate.getLastName()
        );

        signer.setRecipientId(
                "1"
        );

        signer.setRoutingOrder(
                "1"
        );

        signer.setTabs(
                tabs
        );

        Recipients recipients =
                new Recipients();

        recipients.setSigners(
                Collections.singletonList(
                        signer
                )
        );

        EnvelopeDefinition envelope =
                new EnvelopeDefinition();

        envelope.setEmailSubject(
                "TalentGrid Offer Letter"
        );

        envelope.setDocuments(
                Collections.singletonList(
                        document
                )
        );

        envelope.setRecipients(
                recipients
        );

        envelope.setStatus(
                "sent"
        );

        return envelope;
    }
}