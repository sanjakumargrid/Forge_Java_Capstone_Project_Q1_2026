package offerService.offerTemplate.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import offerService.offer.entity.Offer;
import offerService.offer.entity.OfferTemplate;
import offerService.offer.repository.OfferTemplateRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfferTemplateServiceImpl implements OfferTemplateService {

    private final OfferTemplateRepository repository;

    @Override
    public String generateOfferContent(
            Offer offer,
            String templateName
    ) {

        OfferTemplate template =
                repository.findByTemplateName(templateName)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Template not found"
                                )
                        );

        String content = template.getTemplateContent();

        String candidateName =
                "Candidate for application " + offer.getApplicationId();

        return content
                .replace("{{candidateName}}", candidateName)
                .replace("{{role}}", offer.getRole())
                .replace("{{baseSalary}}", String.valueOf(offer.getBaseSalary()))
                .replace("{{bonus}}", String.valueOf(offer.getBonus()))
                .replace("{{equity}}", String.valueOf(offer.getEquity()))
                .replace("{{joiningDate}}", String.valueOf(offer.getJoiningDate()));
    }
}
