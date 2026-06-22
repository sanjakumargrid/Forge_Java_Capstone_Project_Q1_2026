package com.talentgrid.workforce.hmapproval.service.impl;

import com.talentgrid.workforce.hmapproval.dto.HmAcceptRequest;
import com.talentgrid.workforce.hmapproval.dto.HmNominatedEngineerResponse;
import com.talentgrid.workforce.hmapproval.dto.HmRejectRequest;
import com.talentgrid.workforce.hmapproval.dto.HmReviewOutcomeResponse;
import com.talentgrid.workforce.hmapproval.dto.MatchAcceptedPayload;
import com.talentgrid.workforce.hmapproval.dto.MatchRejectedPayload;
import com.talentgrid.workforce.hmapproval.kafka.HmApprovalKafkaProducer;
import com.talentgrid.workforce.hmapproval.service.HmApprovalService;
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

    private final InternalMatchRepository internalMatchRepository;
    private final RmgService rmgService;
    private final HmApprovalKafkaProducer kafkaProducer;

    @Override
    public List<HmNominatedEngineerResponse> getPendingNominationsForDemand(Long demandId) {
        log.info("Fetching PENDING_REVIEW nominations for demandId={}", demandId);
        return internalMatchRepository.findByDemandIdAndIsDeletedFalse(demandId).stream()
                .filter(m -> MatchStatus.PENDING_REVIEW.equals(m.getMatchStatus()))
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

        // 3. Accept this nomination
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

        // 5. Transition demand → FILLED_INTERNAL
        try {
            rmgService.updateDemandStatus(
                    match.getDemandId(),
                    "FILLED_INTERNAL",
                    "FILLED_INTERNAL",
                    "Demand filled internally via HM acceptance of matchId=" + matchId
            );
        } catch (Exception ex) {
            log.warn("Could not transition demand {} to FILLED_INTERNAL: {}", match.getDemandId(), ex.getMessage());
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
