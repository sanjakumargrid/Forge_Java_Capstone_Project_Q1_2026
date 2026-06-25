package offerService.offer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import offerService.offer.dto.ApprovalChainRequestDto;
import offerService.offer.dto.ApprovalStepDto;
import offerService.offer.entity.Offer;
import offerService.offer.enums.Status;
import offerService.offer.service.OfferService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @PreAuthorize("hasAnyAuthority('OFFER_CREATE', 'ADMIN', 'ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<Offer> createOffer(
            @Valid @RequestBody Offer offer
    ) {
        Offer createdOffer = offerService.createOffer(offer);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdOffer);
    }

    @PreAuthorize("hasAnyAuthority('OFFER_VIEW', 'ADMIN', 'ROLE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<Offer> getOfferById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                offerService.getOfferById(id)
        );
    }

    @PreAuthorize("hasAnyAuthority('OFFER_VIEW', 'ADMIN', 'ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<Offer>> getAllOffers(
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) Status status,
            Pageable pageable
    ) {
        Page<Offer> offers = offerService.getAllOffers(
                applicationId,
                status,
                pageable
        );

        return ResponseEntity.ok(offers);
    }

    @PreAuthorize("hasAnyAuthority('OFFER_UPDATE', 'ADMIN', 'ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Offer> updateOffer(
            @PathVariable Long id,
            @Valid @RequestBody Offer offer
    ) {
        return ResponseEntity.ok(
                offerService.updateOffer(id, offer)
        );
    }

    @PreAuthorize("hasAnyAuthority('OFFER_UPDATE', 'OFFER_SEND', 'ADMIN', 'ROLE_ADMIN')")
    @PatchMapping("/{id}/send")
    public ResponseEntity<Offer> sendOffer(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                offerService.sendOffer(id)
        );
    }

    @PreAuthorize("hasAnyAuthority('OFFER_UPDATE', 'OFFER_SIGN', 'ADMIN', 'ROLE_ADMIN')")
    @PatchMapping("/{id}/accept")
    public ResponseEntity<Offer> acceptOffer(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                offerService.acceptOffer(id)
        );
    }

    @PreAuthorize("hasAnyAuthority('OFFER_UPDATE', 'OFFER_SIGN', 'ADMIN', 'ROLE_ADMIN')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<Offer> rejectOffer(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                offerService.rejectOffer(id)
        );
    }

    @PreAuthorize("hasAnyAuthority('OFFER_UPDATE', 'ADMIN', 'ROLE_ADMIN')")
    @PatchMapping("/{id}/expire")
    public ResponseEntity<Offer> expireOffer(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                offerService.expireOffer(id)
        );
    }

    @PreAuthorize("hasAnyAuthority('OFFER_DELETE', 'ADMIN', 'ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffer(
            @PathVariable Long id
    ) {
        offerService.deleteOffer(id);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('OFFER_UPDATE', 'ADMIN', 'ROLE_ADMIN')")
    @PutMapping("/{id}/approval-chain")
    public ResponseEntity<Offer> saveApprovalChain(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalChainRequestDto request
    ) {
        return ResponseEntity.ok(
                offerService.saveApprovalChain(id, request)
        );
    }

    @PreAuthorize("hasAnyAuthority('OFFER_UPDATE', 'OFFER_APPROVE', 'ADMIN', 'ROLE_ADMIN')")
    @PatchMapping("/{id}/submit-approval")
    public ResponseEntity<Offer> submitForApproval(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                offerService.submitForApproval(id)
        );
    }

    @PreAuthorize("hasAnyAuthority('OFFER_UPDATE', 'OFFER_APPROVE', 'ADMIN', 'ROLE_ADMIN')")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<Offer> approveCurrentStep(
            @PathVariable Long id,
            @RequestParam String approverEmail
    ) {
        return ResponseEntity.ok(
                offerService.approveCurrentStep(id, approverEmail)
        );
    }

    @PreAuthorize("hasAnyAuthority('OFFER_UPDATE', 'OFFER_APPROVE', 'ADMIN', 'ROLE_ADMIN')")
    @PatchMapping("/{id}/reject-approval")
    public ResponseEntity<Offer> rejectApproval(
            @PathVariable Long id,
            @RequestParam String approverEmail,
            @RequestParam String comments
    ) {
        return ResponseEntity.ok(
                offerService.rejectApproval(id, approverEmail, comments)
        );
    }

    @PreAuthorize("hasAnyAuthority('OFFER_VIEW', 'ADMIN', 'ROLE_ADMIN')")
    @GetMapping("/{id}/approval-chain")
    public ResponseEntity<List<ApprovalStepDto>> getApprovalChain(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                offerService.getApprovalChain(id)
        );
    }
}