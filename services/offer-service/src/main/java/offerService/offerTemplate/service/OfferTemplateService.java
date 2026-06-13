package offerService.offerTemplate.service;


import offerService.offer.entity.Offer;

public interface OfferTemplateService {

    String generateOfferContent(
            Offer offer,
            String templateName
    );
}