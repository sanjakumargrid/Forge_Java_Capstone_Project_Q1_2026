package offerService.offer.service;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import offerService.kafka.producer.OfferEventProducer;
import offerService.offer.client.ApplicationClient;
import offerService.offer.dto.ApplicationDto;
import offerService.offer.dto.ApprovalChainRequestDto;
import offerService.offer.dto.ApprovalStep;
import offerService.offer.dto.ApprovalStepDto;
import offerService.offer.dto.DocuSignWebhookDto;
import offerService.offer.entity.Offer;
import offerService.offer.enums.Status;
import offerService.offer.integration.DocuSignClient;
import offerService.offer.repository.OfferRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.audit.dto.AuditAction;
import com.talentgrid.audit.dto.AuditLogPayload;


import java.util.Map;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


@Service
@RequiredArgsConstructor
public class OfferServiceImpl implements OfferService {


    private final OfferRepository offerRepository;


    private final ApplicationClient applicationClient;


    private final DocuSignClient docuSignClient;


    private final AuditLogClient auditLogClient;


    private final OfferEventProducer offerEventProducer;


    @Override
    @Transactional
    public Offer createOffer(Offer offer) {


        if (offer.getApplicationId() == null) {
            throw new IllegalArgumentException("Application ID is required");
        }


        ApplicationDto applicationDto =
                applicationClient.getApplication(offer.getApplicationId());


        if (applicationDto == null || applicationDto.getApplicationId() == null) {
            throw new EntityNotFoundException(
                    "Application not found with id: " + offer.getApplicationId()
            );
        }


        String currentStage = applicationDto.getCurrentStage();


        if (!"OFFERED".equalsIgnoreCase(currentStage)
                && !"FINAL_ROUND".equalsIgnoreCase(currentStage)) {
            throw new IllegalStateException(
                    "Offer can be created only when application is in FINAL_ROUND or OFFERED stage. Current stage is "
                            + currentStage
            );
        }


        if ("FINAL_ROUND".equalsIgnoreCase(currentStage)) {
            applicationClient.moveApplicationStage(
                    offer.getApplicationId(),
                    "OFFERED",
                    "Offer created"
            );
        }


        if (offer.getOfferStatus() == null) {
            offer.setOfferStatus(Status.DRAFT);
        }


        Offer savedOffer = offerRepository.save(offer);


        offerEventProducer.publishCreated(savedOffer);


        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("OFFER")
                        .entityId(savedOffer.getId())
                        .action(AuditAction.CREATE)
                        .afterState(Map.of(
                                "applicationId", savedOffer.getApplicationId(),
                                "status", savedOffer.getOfferStatus().name()
                        ))
                        .serviceName("offer-service")
                        .endpoint("/api/offers")
                        .build()
        );






        return savedOffer;
    }


    @Override
    @Transactional(readOnly = true)
    public Offer getOfferById(Long id) {


        return offerRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Offer not found with id: " + id
                        )
                );
    }


    @Override
    @Transactional(readOnly = true)
    public Page<Offer> getAllOffers(
            Long applicationId,
            Status status,
            Pageable pageable
    ) {


        if (applicationId != null && status != null) {
            return offerRepository.findByApplicationIdAndOfferStatus(
                    applicationId,
                    status,
                    pageable
            );
        }


        if (applicationId != null) {
            return offerRepository.findByApplicationId(
                    applicationId,
                    pageable
            );
        }


        if (status != null) {
            return offerRepository.findByOfferStatus(
                    status,
                    pageable
            );
        }


        return offerRepository.findAll(pageable);
    }


    @Override
    @Transactional
    public Offer updateOffer(
            Long id,
            Offer updatedOffer
    ) {


        Offer existingOffer = getOfferById(id);


        if (existingOffer.getOfferStatus() != Status.DRAFT) {
            throw new IllegalStateException(
                    "Only draft offers can be updated"
            );
        }


        existingOffer.setRole(updatedOffer.getRole());
        existingOffer.setBaseSalary(updatedOffer.getBaseSalary());
        existingOffer.setBonus(updatedOffer.getBonus());
        existingOffer.setEquity(updatedOffer.getEquity());
        existingOffer.setJoiningDate(updatedOffer.getJoiningDate());
        existingOffer.setEmploymentType(updatedOffer.getEmploymentType());
        existingOffer.setExpiresAt(updatedOffer.getExpiresAt());


        Offer savedOffer = offerRepository.save(existingOffer);


        offerEventProducer.publishUpdated(savedOffer);


        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("OFFER")
                        .entityId(savedOffer.getId())
                        .action(AuditAction.UPDATE)
                        .afterState(Map.of(
                                "status", savedOffer.getOfferStatus().name()
                        ))
                        .serviceName("offer-service")
                        .endpoint("/api/offers/" + id)
                        .build()
        );






        return savedOffer;
    }


    @Override
    @Transactional
    public Offer sendOffer(Long id) {


        Offer offer = getOfferById(id);


        if (offer.getOfferStatus() != Status.APPROVED) {
            throw new IllegalStateException(
                    "Only approved offers can be sent"
            );
        }


        String envelopeId = docuSignClient.createEnvelope(offer);


        offer.setDocuSignId(envelopeId);
        offer.setOfferStatus(Status.SENT);
        offer.setSentAt(LocalDateTime.now());


        Offer savedOffer = offerRepository.save(offer);


        offerEventProducer.publishSent(savedOffer);


        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("OFFER")
                        .entityId(savedOffer.getId())
                        .action(AuditAction.UPDATE)
                        .afterState(Map.of(
                                "status", savedOffer.getOfferStatus().name()
                        ))
                        .serviceName("offer-service")
                        .endpoint("/api/offers/" + id + "/send")
                        .build()
        );


        return savedOffer;
    }


    @Override
    @Transactional
    public Offer acceptOffer(Long id) {


        Offer offer = getOfferById(id);


        if (offer.getOfferStatus() != Status.SENT) {
            throw new IllegalStateException(
                    "Only sent offers can be accepted"
            );
        }


        offer.setOfferStatus(Status.SIGNED);
        offer.setSignedAt(LocalDateTime.now());


        Offer savedOffer = offerRepository.save(offer);


        offerEventProducer.publishSigned(savedOffer);


        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("OFFER")
                        .entityId(savedOffer.getId())
                        .action(AuditAction.UPDATE)
                        .afterState(Map.of(
                                "status", savedOffer.getOfferStatus().name()
                        ))
                        .serviceName("offer-service")
                        .endpoint("/api/offers/" + id + "/accept")
                        .build()
        );


        applicationClient.moveApplicationStage(
                savedOffer.getApplicationId(),
                "HIRED",
                "Offer signed by candidate"
        );


        return savedOffer;
    }


    @Override
    @Transactional
    public Offer rejectOffer(Long id) {


        Offer offer = getOfferById(id);


        if (offer.getOfferStatus() != Status.SENT) {
            throw new IllegalStateException(
                    "Only sent offers can be rejected"
            );
        }


        offer.setOfferStatus(Status.REJECTED);
        offer.setRejectedAt(LocalDateTime.now());


        Offer savedOffer = offerRepository.save(offer);


        offerEventProducer.publishRejected(
                savedOffer,
                savedOffer.getRejectionReason()
        );


        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("OFFER")
                        .entityId(savedOffer.getId())
                        .action(AuditAction.UPDATE)
                        .afterState(Map.of(
                                "status", savedOffer.getOfferStatus().name()
                        ))
                        .serviceName("offer-service")
                        .endpoint("/api/offers/" + id + "/reject")
                        .build()
        );


        return savedOffer;
    }


    @Override
    @Transactional
    public Offer expireOffer(Long id) {


        Offer offer = getOfferById(id);


        if (offer.getOfferStatus() == Status.SIGNED) {
            throw new IllegalStateException(
                    "Signed offer cannot expire"
            );
        }


        offer.setOfferStatus(Status.EXPIRED);


        Offer savedOffer = offerRepository.save(offer);


        offerEventProducer.publishExpired(savedOffer);


        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("OFFER")
                        .entityId(savedOffer.getId())
                        .action(AuditAction.UPDATE)
                        .afterState(Map.of(
                                "status", savedOffer.getOfferStatus().name()
                        ))
                        .serviceName("offer-service")
                        .endpoint("/api/offers/" + id + "/expire")
                        .build()
        );


        return savedOffer;
    }


    @Override
    @Transactional
    public void deleteOffer(Long id) {


        Offer offer = getOfferById(id);


        offerRepository.delete(offer);
        offerEventProducer.publishDeleted(offer);
        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("OFFER")
                        .entityId(offer.getId())
                        .action(AuditAction.DELETE)
                        .serviceName("offer-service")
                        .endpoint("/api/offers/" + id)
                        .build()
        );
    }


    @Override
    @Transactional
    public Offer saveApprovalChain(
            Long offerId,
            ApprovalChainRequestDto request
    ) {


        Offer offer = getOfferById(offerId);


        if (offer.getOfferStatus() != Status.DRAFT) {
            throw new IllegalStateException(
                    "Approval chain can be changed only for draft offers"
            );
        }


        List<ApprovalStepDto> steps =
                new ArrayList<>(request.getApprovalSteps());


        steps.sort(Comparator.comparing(ApprovalStepDto::getStepOrder));


        if (steps.isEmpty()) {
            throw new IllegalArgumentException(
                    "Approval chain is required"
            );
        }


        for (int i = 0; i < steps.size(); i++) {


            ApprovalStepDto step = steps.get(i);


            if (step.getStepOrder() == null || step.getStepOrder() != i + 1) {
                throw new IllegalArgumentException(
                        "Approval steps must be ordered sequentially from 1"
                );
            }


            if (step.getApproverEmail() == null
                    || step.getApproverEmail().isBlank()) {
                throw new IllegalArgumentException(
                        "Approver email is required for every step"
                );
            }


            if (step.getApproved() == null) {
                step.setApproved(false);
            }
        }


        List<ApprovalStep> chain =
                steps.stream()
                        .map(dto -> {
                            ApprovalStep step = new ApprovalStep();


                            step.setOrderNumber(dto.getStepOrder());
                            step.setApproverEmail(dto.getApproverEmail());
                            step.setApproverName(dto.getApproverName());
                            step.setApproved(false);
                            step.setApprovedBy(null);
                            step.setComments(null);


                            return step;
                        })
                        .toList();


        offer.setApprovalChain(chain);
        offer.setCurrentApprovalStep(0);


        Offer savedOffer = offerRepository.save(offer);


        offerEventProducer.publishUpdated(savedOffer);


        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("OFFER")
                        .entityId(savedOffer.getId())
                        .action(AuditAction.UPDATE)
                        .afterState(Map.of(
                                "approvalSteps", savedOffer.getApprovalChain().size()
                        ))
                        .serviceName("offer-service")
                        .endpoint("/api/offers/" + offerId + "/approval-chain")
                        .build()
        );


        return savedOffer;
    }


    @Override
    @Transactional
    public Offer submitForApproval(Long offerId) {


        Offer offer = getOfferById(offerId);


        if (offer.getApprovalChain() == null
                || offer.getApprovalChain().isEmpty()) {
            throw new IllegalStateException(
                    "Approval chain is required before submitting for approval"
            );
        }


        if (offer.getOfferStatus() != Status.DRAFT) {
            throw new IllegalStateException(
                    "Only draft offers can be submitted for approval"
            );
        }


        offer.setOfferStatus(Status.PENDING_APPROVAL);
        offer.setCurrentApprovalStep(1);


        Offer savedOffer = offerRepository.save(offer);


        offerEventProducer.publishSubmittedForApproval(savedOffer);


        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("OFFER")
                        .entityId(savedOffer.getId())
                        .action(AuditAction.UPDATE)
                        .afterState(Map.of(
                                "status", savedOffer.getOfferStatus().name()
                        ))
                        .serviceName("offer-service")
                        .endpoint("/api/offers/" + offerId + "/submit-approval")
                        .build()
        );


        return savedOffer;
    }


    @Override
    @Transactional
    public Offer approveCurrentStep(
            Long offerId,
            String approverEmail
    ) {


        Offer offer = getOfferById(offerId);


        if (offer.getOfferStatus() != Status.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Offer is not pending approval"
            );
        }


        List<ApprovalStep> steps = offer.getApprovalChain();


        if (steps == null || steps.isEmpty()) {
            throw new IllegalStateException(
                    "Approval chain not configured"
            );
        }


        Integer currentStepNumber = offer.getCurrentApprovalStep();


        if (currentStepNumber == null || currentStepNumber < 1) {
            throw new IllegalStateException(
                    "Offer is not currently in approval flow"
            );
        }


        int currentIndex = currentStepNumber - 1;


        if (currentIndex >= steps.size()) {
            throw new IllegalStateException(
                    "Offer already fully approved"
            );
        }


        ApprovalStep currentStep = steps.get(currentIndex);


        if (!currentStep.getApproverEmail().equalsIgnoreCase(approverEmail)) {
            throw new IllegalStateException(
                    "Only the current approver can approve this step"
            );
        }


        currentStep.setApproved(true);
        currentStep.setApprovedBy(approverEmail);


        boolean allApproved =
                steps.stream()
                        .allMatch(step -> Boolean.TRUE.equals(step.getApproved()));


        if (allApproved) {
            offer.setOfferStatus(Status.APPROVED);
            offer.setApprovedBy(approverEmail);
            offer.setApprovedAt(LocalDateTime.now());
            offer.setCurrentApprovalStep(steps.size());
        } else {
            offer.setCurrentApprovalStep(currentStepNumber + 1);
        }


        Offer savedOffer = offerRepository.save(offer);


        if (savedOffer.getOfferStatus() == Status.APPROVED) {
            offerEventProducer.publishApproved(savedOffer);
        }


        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("OFFER")
                        .entityId(savedOffer.getId())
                        .action(AuditAction.UPDATE)
                        .afterState(Map.of(
                                "status", savedOffer.getOfferStatus().name(),
                                "currentStep", savedOffer.getCurrentApprovalStep()
                        ))
                        .serviceName("offer-service")
                        .endpoint("/api/offers/" + offerId + "/approve")
                        .build()
        );


        return savedOffer;
    }


    @Override
    @Transactional
    public Offer approveOffer(
            Long offerId,
            String approverEmail
    ) {
        return approveCurrentStep(offerId, approverEmail);
    }


    @Override
    @Transactional
    public Offer rejectApproval(
            Long offerId,
            String approverEmail,
            String comments
    ) {


        Offer offer = getOfferById(offerId);


        if (offer.getOfferStatus() != Status.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Only pending approval offers can be rejected"
            );
        }


        offer.setOfferStatus(Status.REJECTED);
        offer.setRejectedBy(approverEmail);
        offer.setRejectedAt(LocalDateTime.now());
        offer.setRejectionReason(comments);
        offer.setCurrentApprovalStep(0);


        Offer savedOffer = offerRepository.save(offer);


        offerEventProducer.publishApprovalRejected(
                savedOffer,
                comments
        );


        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("OFFER")
                        .entityId(savedOffer.getId())
                        .action(AuditAction.UPDATE)
                        .afterState(Map.of(
                                "status", savedOffer.getOfferStatus().name()
                        ))
                        .serviceName("offer-service")
                        .endpoint("/api/offers/" + offerId + "/reject-approval")
                        .build()
        );


        return savedOffer;
    }


    @Override
    @Transactional(readOnly = true)
    public List<ApprovalStepDto> getApprovalChain(Long offerId) {


        Offer offer = getOfferById(offerId);


        List<ApprovalStep> steps = offer.getApprovalChain();


        if (steps == null) {
            return List.of();
        }


        return steps.stream()
                .map(step -> {
                    ApprovalStepDto dto = new ApprovalStepDto();


                    dto.setStepOrder(step.getOrderNumber());
                    dto.setApproverEmail(step.getApproverEmail());
                    dto.setApproverName(step.getApproverName());
                    dto.setApproved(step.getApproved());
                    dto.setApprovedBy(step.getApprovedBy());
                    dto.setComments(step.getComments());


                    return dto;
                })
                .toList();
    }


    @Override
    @Transactional
    public void handleDocuSignWebhook(DocuSignWebhookDto dto) {


        if (dto == null || dto.getEnvelopeId() == null) {
            throw new IllegalArgumentException(
                    "Envelope ID is required"
            );
        }


        Offer offer =
                offerRepository
                        .findByDocuSignId(dto.getEnvelopeId())
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Offer not found for envelope id: "
                                                + dto.getEnvelopeId()
                                )
                        );


        if ("completed".equalsIgnoreCase(dto.getStatus())) {


            offer.setOfferStatus(Status.SIGNED);
            offer.setSignedAt(LocalDateTime.now());


            Offer savedOffer = offerRepository.save(offer);


            offerEventProducer.publishSigned(savedOffer);


            auditLogClient.logAction(
                    AuditLogPayload.builder()
                            .entityType("OFFER")
                            .entityId(savedOffer.getId())
                            .action(AuditAction.UPDATE)
                            .afterState(Map.of(
                                    "status", savedOffer.getOfferStatus().name()
                            ))
                            .serviceName("offer-service")
                            .endpoint("/api/offers/docusign/webhook")
                            .build()
            );


            applicationClient.moveApplicationStage(
                    savedOffer.getApplicationId(),
                    "HIRED",
                    "Offer signed through DocuSign webhook"
            );


            return;
        }


        if ("declined".equalsIgnoreCase(dto.getStatus())) {


            offer.setOfferStatus(Status.REJECTED);
            offer.setRejectedAt(LocalDateTime.now());


            Offer savedOffer = offerRepository.save(offer);


            offerEventProducer.publishRejected(
                    savedOffer,
                    "Declined through DocuSign"
            );


            auditLogClient.logAction(
                    AuditLogPayload.builder()
                            .entityType("OFFER")
                            .entityId(offer.getId())
                            .action(AuditAction.UPDATE)
                            .afterState(Map.of(
                                    "status", offer.getOfferStatus().name()
                            ))
                            .serviceName("offer-service")
                            .endpoint("/api/offers/docusign/webhook")
                            .build()
            );


            return;
        }


        if ("voided".equalsIgnoreCase(dto.getStatus())) {


            offer.setOfferStatus(Status.EXPIRED);


            Offer savedOffer = offerRepository.save(offer);


            offerEventProducer.publishExpired(savedOffer);


            auditLogClient.logAction(
                    AuditLogPayload.builder()
                            .entityType("OFFER")
                            .entityId(offer.getId())
                            .action(AuditAction.UPDATE)
                            .afterState(Map.of(
                                    "status", offer.getOfferStatus().name()
                            ))
                            .serviceName("offer-service")
                            .endpoint("/api/offers/docusign/webhook")
                            .build()
            );
        }
    }
}

