package com.talentgrid.workforce.rmgnomination.service;

import com.talentgrid.workforce.skillgapheatmap.client.DemandServiceClient;
import com.talentgrid.workforce.skillgapheatmap.provider.model.DemandServiceResponse;
import feign.FeignException;
import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import com.talentgrid.workforce.engineerprofilemanagement.repository.InternalEmployeeRepository;
import com.talentgrid.workforce.rmgdashboard.client.DemandClient;
import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
import com.talentgrid.workforce.rmgdashboard.service.RmgService;
import com.talentgrid.workforce.rmgnomination.dto.NominationRequest;
import com.talentgrid.workforce.rmgnomination.dto.NominationResponse;
import com.talentgrid.workforce.rmgnomination.entity.InternalMatch;
import com.talentgrid.workforce.rmgnomination.enums.MatchStatus;
import com.talentgrid.workforce.rmgnomination.enums.NominationType;
import com.talentgrid.workforce.rmgnomination.repository.InternalMatchRepository;
import com.talentgrid.workforce.airmgnomination.repository.DemandRecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NominationService {

    private final InternalEmployeeRepository internalEmployeeRepository;
    private final InternalMatchRepository internalMatchRepository;
    private final DemandClient demandClient;
    private final DemandServiceClient demandServiceClient;
    private final RmgService rmgService;
    private final DemandRecommendationRepository demandRecommendationRepository;

    @Transactional
    public NominationResponse nominate(NominationRequest request) {
        log.info("Processing nomination for employeeId={} on demandId={}", request.getEmployeeId(), request.getDemandId());

        // 1. Validate Employee Exists
        InternalEmployee employee = internalEmployeeRepository.findById(request.getEmployeeId())
                .filter(e -> !e.getIsDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Engineer not found: " + request.getEmployeeId()));
        Long employeeId = employee.getId();

        // 2. Validate demand exists and is in nominatable state.
        //    On first RM nomination from APPROVED, auto-transition to INTERNAL_SEARCH.
        DemandDto demand = demandClient.getDemandById(request.getDemandId());
        String currentStatus = normalizedStatus(demand.getStatus());

        // 3. Check for duplicate nomination by the same engineer.
        boolean alreadyNominated = internalMatchRepository.existsByEmployee_IdAndDemandIdAndIsDeletedFalse(
                employeeId, request.getDemandId());

        if (alreadyNominated) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Engineer already nominated for this demand");
        }

        // 4. One engineer can be nominated to maximum two active demands.
        long activeDemandCount = internalMatchRepository.countByEmployee_IdAndIsDeletedFalse(employeeId);
        if (activeDemandCount >= 2) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Engineer already has maximum 2 active demand nominations");
        }

        if ("APPROVED".equals(currentStatus)) {
            demand = transitionApprovedToInternalSearch(request.getDemandId(), demand);
            currentStatus = normalizedStatus(demand.getStatus());
        }

        if (!"INTERNAL_SEARCH".equals(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Demand is not available for nomination. Expected status APPROVED or INTERNAL_SEARCH, found: "
                            + demand.getStatus());
        }

        // 5. Validate against the engineer's current accepted utilisation.
        // RMG only validates here; allocation is recorded later in HM acceptance flow.
        int liveUtil = safeInt(employee.getUtilisationPct());
        int requestedAllocation = request.getAllocationPercentage();
        if (liveUtil + requestedAllocation > 100) {
            String errorMsg = String.format("Engineer utilisation would exceed 100%%. Current: %d%%, Requested: %d%%, Total would be: %d%%",
                    liveUtil, requestedAllocation, liveUtil + requestedAllocation);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, errorMsg);
        }

        // 6. Write InternalMatch
        NominationType nominationType = request.getNominationType() != null
                ? request.getNominationType()
                : NominationType.MANUAL;

        BigDecimal matchScore = null;
        Integer fitPercentage = null;

        if (nominationType == NominationType.AI_ASSISTED) {
            log.info("[NOMINATION] AI_ASSISTED nomination — looking up recommendation for demandId={} employeeId={}",
                    request.getDemandId(), employee.getId());
            var recommendationOpt = demandRecommendationRepository.findByDemandIdAndEmployee_Id(
                    request.getDemandId(), employee.getId());
            if (recommendationOpt.isPresent()) {
                double score = recommendationOpt.get().getAiScore();
                matchScore = BigDecimal.valueOf(score);
                fitPercentage = (int) Math.round(score);
                log.info("[NOMINATION] Found AI recommendation — aiScore={}, matchScore={}, fitPercentage={}",
                        score, matchScore, fitPercentage);
            } else {
                log.warn("[NOMINATION] No recommendation found in demand_recommendation table for demandId={} employeeId={}",
                        request.getDemandId(), employee.getId());
            }
        }

        InternalMatch match = InternalMatch.builder()
                .employee(employee)
                .demandId(request.getDemandId())
                .nominatedBy(request.getNominatedBy())
                .nominationReasonByRmg(request.getNominationReasonByRmg())
                .notes(request.getNotes())
                .nominationType(nominationType)
                .matchStatus(MatchStatus.PENDING_REVIEW)
                .matchScore(matchScore)
                .fitPercentage(fitPercentage)
                .build();

        match = internalMatchRepository.save(match);

        // 7. Async Kafka Publish (NOT IMPLEMENTED NOW)
        // TODO: After DB transaction commits, publish BaseEvent<MatchNominatedEvent>
        //       with eventType="match.nominated" to topic "internal-match-events".
        //       Keep publish async / fire-and-forget so HTTP response is not blocked.

        // 8. Return NominationResponse
        return NominationResponse.builder()
                .matchId(match.getId())
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeId())
                .employeeName(employee.getName())
                .employeeEmail(employee.getEmail())
                .level(employee.getLevel())
                .availabilityDate(employee.getAvailabilityDate())
                .location(employee.getLocation())
                .contractType(employee.getContractType())
                .skills(employee.getSkills() != null ? Arrays.asList(employee.getSkills()) : List.of())
                .demandId(request.getDemandId())
                .matchStatus(match.getMatchStatus().name())
                .nominationType(match.getNominationType().name())
                .nominatedAt(match.getNominatedAt())
                .utilisationAfter(liveUtil)
                .matchScore(match.getMatchScore())
                .fitPercentage(match.getFitPercentage())
                .build();
    }

    public List<NominationResponse> getNominationsByDemand(Long demandId) {
        return internalMatchRepository.findByDemandIdAndIsDeletedFalse(demandId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<NominationResponse> getNominationsByEngineer(Long employeeId) {
        return internalMatchRepository.findByEmployee_IdAndIsDeletedFalse(employeeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DemandServiceResponse getDemandDetailsById(Long demandId) {
        try {
            DemandServiceResponse demand = demandServiceClient.getDemandById(demandId);
            if (demand == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demand not found: " + demandId);
            }
            return demand;
        } catch (FeignException ex) {
            HttpStatus statusCode = HttpStatus.resolve(ex.status());
            HttpStatus resolved = statusCode != null ? statusCode : HttpStatus.BAD_GATEWAY;
            String message = ex.contentUTF8() != null && !ex.contentUTF8().isBlank()
                    ? ex.contentUTF8()
                    : ex.getMessage();
            throw new ResponseStatusException(resolved, message, ex);
        }
    }

    public DemandDto moveDemandToInternalSearch(Long demandId) {
        DemandDto demand = demandClient.getDemandById(demandId);
        if (demand == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demand not found: " + demandId);
        }

        String currentStatus = normalizedStatus(demand.getStatus());
        if ("INTERNAL_SEARCH".equals(currentStatus)) {
            return demand;
        }

        // If demand is pending approval, move it to APPROVED first, then to INTERNAL_SEARCH.
        if ("PENDING_APPROVAL".equals(currentStatus)) {
            demand = rmgService.updateDemandStatus(
                    demandId,
                    "APPROVED",
                    null,
                    "Auto-transitioned to APPROVED before moving to INTERNAL_SEARCH from RMG nomination API"
            );
            currentStatus = normalizedStatus(demand.getStatus());
        }

        if ("APPROVED".equals(currentStatus)) {
            return rmgService.updateDemandStatus(
                    demandId,
                    "INTERNAL_SEARCH",
                    null,
                    "Transitioned by RMG nomination API"
            );
        }

        throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Demand transition to INTERNAL_SEARCH is allowed only from PENDING_APPROVAL or APPROVED. Current status: "
                        + demand.getStatus()
        );
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizedStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private DemandDto transitionApprovedToInternalSearch(Long demandId, DemandDto currentDemand) {
        try {
            return rmgService.updateDemandStatus(
                    demandId,
                    "INTERNAL_SEARCH",
                    null,
                    "Auto-transitioned from APPROVED to INTERNAL_SEARCH on first RM nomination"
            );
        } catch (ResponseStatusException ex) {
            // Concurrency-safe fallback:
            // if another request already moved status to INTERNAL_SEARCH, proceed.
            DemandDto latestDemand = demandClient.getDemandById(demandId);
            if ("INTERNAL_SEARCH".equals(normalizedStatus(latestDemand.getStatus()))) {
                log.info("Demand {} already moved to INTERNAL_SEARCH by concurrent request; continuing nomination", demandId);
                return latestDemand;
            }
            throw ex;
        }
    }

    private NominationResponse mapToResponse(InternalMatch match) {
        InternalEmployee employee = match.getEmployee();
        Integer utilisationAfter = employee != null ? employee.getUtilisationPct() : null;
        return NominationResponse.builder()
                .matchId(match.getId())
                .employeeId(employee != null ? employee.getId() : null)
                .employeeCode(employee != null ? employee.getEmployeeId() : null)
                .employeeName(employee != null ? employee.getName() : null)
                .employeeEmail(employee != null ? employee.getEmail() : null)
                .level(employee != null ? employee.getLevel() : null)
                .availabilityDate(employee != null ? employee.getAvailabilityDate() : null)
                .location(employee != null ? employee.getLocation() : null)
                .contractType(employee != null ? employee.getContractType() : null)
                .skills(employee != null && employee.getSkills() != null ? Arrays.asList(employee.getSkills()) : List.of())
                .demandId(match.getDemandId())
                .matchStatus(match.getMatchStatus().name())
                .nominationType(match.getNominationType().name())
                .nominatedAt(match.getNominatedAt())
                .utilisationAfter(utilisationAfter)
                .matchScore(match.getMatchScore())
                .fitPercentage(match.getFitPercentage())
                .build();
    }
}
