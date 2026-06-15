package offerService.offer.integration;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import offerService.offer.entity.Offer;
import offerService.offer.entity.OfferTemplate;
import offerService.offer.repository.OfferTemplateRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class OfferDocumentService {

    private final OfferTemplateRepository offerTemplateRepository;

    public byte[] generateOfferPdf(Offer offer) {

        String html = buildOfferHtml(offer);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to generate offer PDF",
                    e
            );
        }
    }

    private String buildOfferHtml(Offer offer) {

        String candidateName =
                (offer.getCandidateName() != null && !offer.getCandidateName().isBlank())
                        ? offer.getCandidateName()
                        : "Candidate (Application #" + offer.getApplicationId() + ")";

        String templateContent =
                offerTemplateRepository.findByActiveTrue()
                        .map(OfferTemplate::getTemplateContent)
                        .orElse(getDefaultTemplate());

        return templateContent
                .replace("{{candidateName}}", candidateName)
                .replace("{{role}}", offer.getRole())
                .replace("{{baseSalary}}", offer.getBaseSalary().toPlainString())
                .replace(
                        "{{bonus}}",
                        offer.getBonus() != null
                                ? offer.getBonus().toPlainString()
                                : "0"
                )
                .replace(
                        "{{equity}}",
                        offer.getEquity() != null
                                ? offer.getEquity().toPlainString()
                                : "0"
                )
                .replace("{{joiningDate}}", offer.getJoiningDate().toString())
                .replace("{{employmentType}}", offer.getEmploymentType());
    }

    private String getDefaultTemplate() {

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 40px; color: #333; }
                        h1 { color: #2c3e50; }
                        .section { margin: 20px 0; }
                        .label { font-weight: bold; }
                        .sign-here { margin-top: 60px; border-top: 1px solid #333; width: 200px; }
                    </style>
                </head>
                <body>
                    <h1>Offer Letter</h1>
                    <div class="section">
                        <p>Dear {{candidateName}},</p>
                        <p>We are pleased to extend an offer of employment for the position of
                        <strong>{{role}}</strong>.</p>
                    </div>
                    <div class="section">
                        <p><span class="label">Base Salary:</span> ₹{{baseSalary}} per annum</p>
                        <p><span class="label">Bonus:</span> ₹{{bonus}}</p>
                        <p><span class="label">Equity:</span> {{equity}}%</p>
                        <p><span class="label">Employment Type:</span> {{employmentType}}</p>
                        <p><span class="label">Joining Date:</span> {{joiningDate}}</p>
                    </div>
                    <div class="section">
                        <p>This offer is contingent upon successful completion of background verification.</p>
                        <p>Please sign below to confirm acceptance.</p>
                    </div>
                    <div class="sign-here">
                        <p>{{SIGN_HERE}}</p>
                        <p>Candidate Signature</p>
                    </div>
                </body>
                </html>
                """;
    }
}
