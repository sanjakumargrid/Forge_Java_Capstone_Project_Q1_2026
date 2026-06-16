package offerService.offer.service;

import offerService.offer.dto.ApprovalChainRequestDto;
import offerService.offer.dto.ApprovalStepDto;
import offerService.offer.dto.DocuSignWebhookDto;
import offerService.offer.entity.Offer;
import offerService.offer.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OfferService {

    Offer createOffer(Offer offer);

    Offer getOfferById(Long id);

    Page<Offer> getAllOffers(
            Long applicationId,
            Status status,
            Pageable pageable
    );

    Offer updateOffer(
            Long id,
            Offer offer
    );

    Offer sendOffer(Long id);

    Offer acceptOffer(Long id);

    Offer rejectOffer(Long id);

    Offer expireOffer(Long id);

    void deleteOffer(Long id);

    Offer saveApprovalChain(
            Long offerId,
            ApprovalChainRequestDto request
    );

    Offer submitForApproval(Long offerId);

    Offer approveCurrentStep(
            Long offerId,
            String approverEmail
    );

    Offer approveOffer(
            Long offerId,
            String approverEmail
    );

    Offer rejectApproval(
            Long offerId,
            String approverEmail,
            String comments
    );

    List<ApprovalStepDto> getApprovalChain(Long offerId);

    void handleDocuSignWebhook(DocuSignWebhookDto dto);
}
