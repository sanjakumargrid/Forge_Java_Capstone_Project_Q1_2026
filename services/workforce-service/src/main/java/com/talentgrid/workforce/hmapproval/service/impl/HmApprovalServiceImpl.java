package com.talentgrid.workforce.hmapproval.service.impl;

import com.talentgrid.workforce.hmapproval.client.UserAuthClient;
import com.talentgrid.workforce.hmapproval.dto.HiringTypeResponse;
import com.talentgrid.workforce.hmapproval.dto.HmAcceptRequest;
import com.talentgrid.workforce.hmapproval.dto.HmNominatedEngineerResponse;
import com.talentgrid.workforce.hmapproval.dto.HmRejectRequest;
import com.talentgrid.workforce.hmapproval.dto.HmReviewOutcomeResponse;
import com.talentgrid.workforce.hmapproval.dto.MatchAcceptedPayload;
import com.talentgrid.workforce.hmapproval.dto.MatchRejectedPayload;
import com.talentgrid.workforce.hmapproval.dto.UserRoleResponse;
import com.talentgrid.workforce.hmapproval.kafka.HmApprovalKafkaProducer;
import com.talentgrid.workforce.hmapproval.service.HmApprovalService;
import com.talentgrid.workforce.rmgdashboard.client.DemandClient;
import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
import com.talentgrid.workforce.rmgdashboard.service.RmgService;
import com.talentgrid.workforce.rmgnomination.entity.InternalMatch;
import com.talentgrid.workforce.rmgnomination.enums.MatchStatus;
import com.talentgrid.workforce.rmgnomination.repository.InternalMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HmApprovalServiceImpl implements HmApprovalService {

    private static final String AUTO_REJECT_REASON =
            "Auto-rejected: another engineer was accepted for this demand by the Hiring Manager.";

    private static final String ROLE_RESOURCE_MANAGER = "RESOURCE_MANAGER";
    private static final String ROLE_RECRUITER = "RECRUITER";

    private final InternalMatchRepository internalMatchRepository;
    private final RmgService rmgService;
    private final HmApprovalKafkaProducer kafkaProducer;
    private final DemandClient demandClient;
    private final UserAuthClient userAuthClient;

    @Override
    public List<HmNominatedEngineerResponse> getPendingNominationsForDemand(Long demandId) {
        log.info("Fetching PENDING_REVIEW nominations for demandId={}", demandId);
        
        // FIX: Filter at database level instead of Java stream filtering
        return internalMatchRepository
                .findByDemandIdAndMatchStatusAndIsDeletedFalse(demandId, MatchStatus.PENDING_REVIEW)
                .stream()
                .map(this::toNominatedEngineerResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HmReviewOutcomeResponse acceptNomination(Long matchId, HmAcceptRequest request) {
        log.info("HM accept request: matchId={}, confirmed={}, reviewedBy={}",
                matchId, request.getConfirmed(), request.getReviewedBy());

        // 1. Load and guard the target match
        InternalMatch match = loadPendingMatch(matchId);

        // 2. Confirmation guard — return a prompt if HM hasn't confirmed yet
        if (Boolean.FALSE.equals(request.getConfirmed())) {
            log.info("HM has not confirmed acceptance for matchId={}. Returning prompt.", matchId);
            return HmReviewOutcomeResponse.builder()
                    .matchId(matchId)
                    .demandId(match.getDemandId())
                    .employeeId(match.getEmployee() != null ? match.getEmployee().getId() : null)
                    .employeeName(match.getEmployee() != null ? match.getEmployee().getName() : null)
                    .matchStatus(match.getMatchStatus().name())
                    .message("Are you sure you want to accept this engineer for the demand? " +
                             "Re-send the request with confirmed=true to proceed.")
                    .build();
        }

        // 3. Pre-accept guard — verify engineer doesn't already have 2 ACCEPTED demand assignments
        //    (PENDING_REVIEW on this demand is fine; we check ACCEPTED on *other* demands only)
        long acceptedOnOtherDemands = internalMatchRepository
                .findByEmployee_IdAndIsDeletedFalse(match.getEmployee().getId())
                .stream()
                .filter(m -> MatchStatus.ACCEPTED.equals(m.getMatchStatus())
                          && !match.getDemandId().equals(m.getDemandId()))
                .count();
        if (acceptedOnOtherDemands >= 2) {
            log.warn("Cannot accept matchId={}: engineer {} is already ACCEPTED on {} other demands",
                    matchId, match.getEmployee().getId(), acceptedOnOtherDemands);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot accept: engineer is already committed to 2 other demands. " +
                    "Maximum active demand limit of 2 reached.");
        }

        // 4. Accept this nomination
        LocalDateTime now = LocalDateTime.now();
        match.setMatchStatus(MatchStatus.ACCEPTED);
        match.setApprovalReasonByHm(request.getReason());
        match.setReviewedBy(request.getReviewedBy());
        match.setReviewedAt(now);
        internalMatchRepository.save(match);

        // 4. Auto-reject all other PENDING_REVIEW nominations for the same demand
        List<InternalMatch> otherPending = internalMatchRepository
                .findByDemandIdAndIsDeletedFalse(match.getDemandId())
                .stream()
                .filter(m -> MatchStatus.PENDING_REVIEW.equals(m.getMatchStatus())
                             && !m.getId().equals(matchId))
                .collect(Collectors.toList());

        otherPending.forEach(m -> {
            m.setMatchStatus(MatchStatus.REJECTED);
            m.setRejectionReasonHm(AUTO_REJECT_REASON);
            m.setReviewedBy(request.getReviewedBy());
            m.setReviewedAt(now);
        });
        internalMatchRepository.saveAll(otherPending);

        // 5. Transition demand → FILLED
        try {
            rmgService.updateDemandStatus(
                    match.getDemandId(),
                    "FILLED",
                    "FILLED",
                    "Demand filled internally via HM acceptance of matchId=" + matchId
            );
        } catch (Exception ex) {
            log.error("Failed to transition demand {} to FILLED after HM acceptance of matchId={}: {}",
                    match.getDemandId(), matchId, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Match accepted but demand status could not be updated: " + ex.getMessage(), ex);
        }

        // 6. Kafka: publish accepted event
        kafkaProducer.publishMatchAccepted(
                MatchAcceptedPayload.builder()
                        .matchId(matchId)
                        .demandId(match.getDemandId())
                        .employeeId(match.getEmployee() != null ? match.getEmployee().getId() : null)
                        .employeeName(match.getEmployee() != null ? match.getEmployee().getName() : null)
                        .approvalReasonByHm(request.getReason())
                        .reviewedBy(request.getReviewedBy())
                        .reviewedAt(now)
                        .autoRejectedCount(otherPending.size())
                        .build()
        );

        // 7. Kafka: publish auto-rejected events
        otherPending.forEach(m -> kafkaProducer.publishMatchRejected(
                MatchRejectedPayload.builder()
                        .matchId(m.getId())
                        .demandId(m.getDemandId())
                        .employeeId(m.getEmployee() != null ? m.getEmployee().getId() : null)
                        .employeeName(m.getEmployee() != null ? m.getEmployee().getName() : null)
                        .rejectionReasonHm(AUTO_REJECT_REASON)
                        .reviewedBy(request.getReviewedBy())
                        .reviewedAt(now)
                        .autoRejected(true)
                        .build()
        ));

        return HmReviewOutcomeResponse.builder()
                .matchId(matchId)
                .demandId(match.getDemandId())
                .employeeId(match.getEmployee() != null ? match.getEmployee().getId() : null)
                .employeeName(match.getEmployee() != null ? match.getEmployee().getName() : null)
                .matchStatus(MatchStatus.ACCEPTED.name())
                .reason(request.getReason())
                .reviewedBy(request.getReviewedBy())
                .reviewedAt(now)
                .autoRejectedCount(otherPending.size())
                .build();
    }

    @Override
    @Transactional
    public HmReviewOutcomeResponse rejectNomination(Long matchId, HmRejectRequest request) {
        log.info("HM reject request: matchId={}, reviewedBy={}", matchId, request.getReviewedBy());

        InternalMatch match = loadPendingMatch(matchId);

        LocalDateTime now = LocalDateTime.now();
        match.setMatchStatus(MatchStatus.REJECTED);
        match.setRejectionReasonHm(request.getReason());
        match.setReviewedBy(request.getReviewedBy());
        match.setReviewedAt(now);
        internalMatchRepository.save(match);

        kafkaProducer.publishMatchRejected(
                MatchRejectedPayload.builder()
                        .matchId(matchId)
                        .demandId(match.getDemandId())
                        .employeeId(match.getEmployee() != null ? match.getEmployee().getId() : null)
                        .employeeName(match.getEmployee() != null ? match.getEmployee().getName() : null)
                        .rejectionReasonHm(request.getReason())
                        .reviewedBy(request.getReviewedBy())
                        .reviewedAt(now)
                        .autoRejected(false)
                        .build()
        );

        return HmReviewOutcomeResponse.builder()
                .matchId(matchId)
                .demandId(match.getDemandId())
                .employeeId(match.getEmployee() != null ? match.getEmployee().getId() : null)
                .employeeName(match.getEmployee() != null ? match.getEmployee().getName() : null)
                .matchStatus(MatchStatus.REJECTED.name())
                .reason(request.getReason())
                .reviewedBy(request.getReviewedBy())
                .reviewedAt(now)
                .build();
    }

    @Override
    public HiringTypeResponse getHiringType(Long demandId) {
        log.info("Resolving hiring type for demandId={}", demandId);

        // 1. Fetch the demand to extract the approvedBy user ID
        DemandDto demand;
        try {
            demand = demandClient.getDemandById(demandId);
        } catch (Exception ex) {
            log.warn("Could not fetch demand {}: {}", demandId, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demand not found: " + demandId);
        }

        Long approvedBy = demand.getApprovedBy();
        if (approvedBy == null) {
            log.info("Demand {} has no approvedBy user recorded", demandId);
            return HiringTypeResponse.builder()
                    .demandId(demandId)
                    .hiringType("UNKNOWN")
                    .message("No approver recorded for this demand — hiring type cannot be determined.")
                    .build();
        }

        // 2. Fetch the approver's roles from user-auth-service
        UserRoleResponse approver;
        try {
            approver = userAuthClient.getUserById(approvedBy);
        } catch (Exception ex) {
            log.warn("Could not fetch roles for userId={}: {}", approvedBy, ex.getMessage());
            return HiringTypeResponse.builder()
                    .demandId(demandId)
                    .approvedByUserId(approvedBy)
                    .hiringType("UNKNOWN")
                    .message("Unable to retrieve approver roles (insufficient permissions or user-auth-service error).")
                    .build();
        }

        // 3. Classify based on role
        String resolvedRole = null;
        String hiringType = "UNKNOWN";

        if (approver.getRoles() != null) {
            if (approver.getRoles().contains(ROLE_RESOURCE_MANAGER)) {
                resolvedRole = ROLE_RESOURCE_MANAGER;
                hiringType = "INTERNAL";
            } else if (approver.getRoles().contains(ROLE_RECRUITER)) {
                resolvedRole = ROLE_RECRUITER;
                hiringType = "EXTERNAL";
            }
        }

        // 4. For INTERNAL hires, check the nomination_type of the accepted match
        String nominationMode = null;
        if ("INTERNAL".equals(hiringType)) {
            nominationMode = internalMatchRepository
                    .findByDemandIdAndIsDeletedFalse(demandId)
                    .stream()
                    .filter(m -> MatchStatus.ACCEPTED.equals(m.getMatchStatus()))
                    .findFirst()
                    .map(m -> m.getNominationType().name())
                    .orElse(null);
            log.info("demandId={} nominationMode={}", demandId, nominationMode);
        }

        log.info("demandId={} approvedBy={} role={} hiringType={} nominationMode={}",
                demandId, approvedBy, resolvedRole, hiringType, nominationMode);

        return HiringTypeResponse.builder()
                .demandId(demandId)
                .approvedByUserId(approvedBy)
                .approvedByRole(resolvedRole)
                .hiringType(hiringType)
                .nominationMode(nominationMode)
                .message(buildHiringTypeMessage(hiringType, nominationMode, approver))
                .build();
    }

    private String buildHiringTypeMessage(String hiringType, String nominationMode, UserRoleResponse approver) {
        return switch (hiringType) {
            case "INTERNAL" -> nominationMode != null
                    ? "Demand was approved by a Resource Manager — classified as INTERNAL hiring via " + nominationMode + " nomination."
                    : "Demand was approved by a Resource Manager — classified as INTERNAL hiring (no accepted match yet).";
            case "EXTERNAL" -> "Demand was approved by a Recruiter — classified as EXTERNAL hiring.";
            default -> "Approver '" + (approver.getUsername() != null ? approver.getUsername() : approver.getId())
                    + "' does not hold a RESOURCE_MANAGER or RECRUITER role.";
        };
    }

    private InternalMatch loadPendingMatch(Long matchId) {
        InternalMatch match = internalMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Nomination not found: " + matchId));

        if (Boolean.TRUE.equals(match.getIsDeleted())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nomination not found: " + matchId);
        }

        if (!MatchStatus.PENDING_REVIEW.equals(match.getMatchStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nomination " + matchId + " is already " + match.getMatchStatus().name()
                    + " and cannot be reviewed again.");
        }

        return match;
    }

    private HmNominatedEngineerResponse toNominatedEngineerResponse(InternalMatch match) {
        var employee = match.getEmployee();
        return HmNominatedEngineerResponse.builder()
                .matchId(match.getId())
                .employeeId(employee != null ? employee.getId() : null)
                .employeeCode(employee != null ? employee.getEmployeeId() : null)
                .employeeName(employee != null ? employee.getName() : null)
                .employeeEmail(employee != null ? employee.getEmail() : null)
                .level(employee != null ? employee.getLevel() : null)
                .location(employee != null ? employee.getLocation() : null)
                .contractType(employee != null ? employee.getContractType() : null)
                .availabilityDate(employee != null ? employee.getAvailabilityDate() : null)
                .skills(employee != null && employee.getSkills() != null
                        ? Arrays.asList(employee.getSkills()) : List.of())
                .utilisationPct(employee != null ? employee.getUtilisationPct() : null)
                .demandId(match.getDemandId())
                .matchStatus(match.getMatchStatus().name())
                .nominationReasonByRmg(match.getNominationReasonByRmg())
                .nominatedBy(match.getNominatedBy())
                .nominatedAt(match.getNominatedAt())
                .build();
    }
}
