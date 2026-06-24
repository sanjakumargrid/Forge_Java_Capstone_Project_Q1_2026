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
                        body { font-family: Arial, sans-serif; margin: 40px; color: #333; line-height: 1.6; }
                        h1 { color: #2c3e50; border-bottom: 2px solid #e67e22; padding-bottom: 10px; }
                        h2 { color: #e67e22; font-size: 18px; font-weight: bold; margin-top: 5px; }
                        .section { margin: 25px 0; }
                        .label { font-weight: bold; width: 150px; display: inline-block; }
                        .details { background-color: #f9f9f9; padding: 15px; border-radius: 5px; border-left: 4px solid #2c3e50; }
                        .signature-block { margin-top: 80px; page-break-inside: avoid; }
                        .signature-line { border-bottom: 2px solid #2c3e50; width: 250px; margin-bottom: 10px; }
                        .signature-label { font-weight: bold; font-size: 14px; color: #2c3e50; text-transform: uppercase; letter-spacing: 1px; }
                        .footer { margin-top: 80px; font-size: 12px; color: #7f8c8d; text-align: center; border-top: 1px solid #eee; padding-top: 20px; }
                    </style>
                </head>
                <body>
                    <h1>Offer of Employment</h1>
                    <h2>Grid Dynamics</h2>
                    <div class="section">
                        <p>Dear <strong>{{candidateName}}</strong>,</p>
                        <p>On behalf of <strong>Grid Dynamics</strong>, we are thrilled to extend an offer of employment for the position of <strong>{{role}}</strong>.</p>
                    </div>
                    <div class="section details">
                        <p><span class="label">Base Salary:</span> INR {{baseSalary}} per annum</p>
                        <p><span class="label">Bonus:</span> INR {{bonus}}</p>
                        <p><span class="label">Equity:</span> {{equity}}%</p>
                        <p><span class="label">Employment Type:</span> {{employmentType}}</p>
                        <p><span class="label">Joining Date:</span> {{joiningDate}}</p>
                    </div>
                    <div class="section">
                        <p>This offer is contingent upon successful completion of background verification.</p>
                        <p>We are excited about the prospect of you joining our team at Grid Dynamics! Please sign below to confirm your acceptance.</p>
                    </div>
                    <div class="signature-block">
                        <div class="signature-line"></div>
                        <div class="signature-label">Candidate Signature</div>
                    </div>
                    <div class="footer">
                        Generated by TalentGrid ATS | Grid Dynamics
                    </div>
                </body>
                </html>
                """;
    }
}
