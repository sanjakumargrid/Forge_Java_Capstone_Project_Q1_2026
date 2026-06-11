package com.talentgrid.workforce.rmgnomination.service;

import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import com.talentgrid.workforce.engineerprofilemanagement.repository.InternalEmployeeRepository;
import com.talentgrid.workforce.rmgdashboard.client.DemandClient;
import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
import com.talentgrid.workforce.rmgnomination.dto.NominationRequest;
import com.talentgrid.workforce.rmgnomination.dto.NominationResponse;
import com.talentgrid.workforce.rmgnomination.entity.EmployeeUtilisation;
import com.talentgrid.workforce.rmgnomination.entity.InternalMatch;
import com.talentgrid.workforce.rmgnomination.enums.MatchStatus;
import com.talentgrid.workforce.rmgnomination.enums.NominationType;
import com.talentgrid.workforce.rmgnomination.repository.EmployeeUtilisationRepository;
import com.talentgrid.workforce.rmgnomination.repository.InternalMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NominationService {

    private final InternalEmployeeRepository internalEmployeeRepository;
    private final InternalMatchRepository internalMatchRepository;
    private final EmployeeUtilisationRepository employeeUtilisationRepository;
    private final DemandClient demandClient;

    @Transactional
    public NominationResponse nominate(NominationRequest request) {
        log.info("Processing nomination for employeeId={} on demandId={}", request.getEmployeeId(), request.getDemandId());

        // 1. Validate Employee Exists
        InternalEmployee employee = internalEmployeeRepository.findById(request.getEmployeeId())
                .filter(e -> !e.getIsDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Engineer not found: " + request.getEmployeeId()));
        Long employeeId = employee.getId();

        // 2. Validate demand exists in APPROVED state
        DemandDto approvedDemand = demandClient.getDemandsByStatus("APPROVED").stream()
                .filter(demand -> demand.getDemandId() != null && demand.getDemandId().equals(request.getDemandId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Demand is not available for nomination in APPROVED state: " + request.getDemandId()));

        // 3. Check demand headcount fulfillment
        if (isDemandAlreadyFulfilledInternally(approvedDemand)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Demand is already filled internally for demandId: " + request.getDemandId());
        }

        // 4. Check for Duplicate Nomination
        boolean alreadyNominated = internalMatchRepository.existsByEmployee_IdAndDemandIdAndIsDeletedFalse(
                employeeId, request.getDemandId());
        
        if (alreadyNominated) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Engineer already nominated for this demand");
        }

        // 5. Compute Live Utilisation
        int liveUtil = employeeUtilisationRepository.sumAllocatedPercentageByEmployeeId(employeeId);
        int requestedAllocation = request.getAllocationPercentage();

        if (liveUtil + requestedAllocation > 100) {
            String errorMsg = String.format("Engineer utilisation would exceed 100%%. Current: %d%%, Requested: %d%%, Total would be: %d%%",
                    liveUtil, requestedAllocation, liveUtil + requestedAllocation);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, errorMsg);
        }

        // 6. Write InternalMatch
        InternalMatch match = InternalMatch.builder()
                .employee(employee)
                .demandId(request.getDemandId())
                .nominatedBy(request.getNominatedBy())
                .nominationReasonByRmg(request.getNominationReasonByRmg())
                .notes(request.getNotes())
                .nominationType(NominationType.MANUAL)
                .matchStatus(MatchStatus.PENDING_REVIEW)
                .build();
        
        match = internalMatchRepository.save(match);

        // 7. Write EmployeeUtilisation
        EmployeeUtilisation utilisation = EmployeeUtilisation.builder()
                .employee(employee)
                .demandId(request.getDemandId())
                .allocatedPercentage(requestedAllocation)
                .build();
        
        employeeUtilisationRepository.save(utilisation);

        // 8. Update Denormalised utilisation_pct on InternalEmployee
        int newUtilPct = liveUtil + requestedAllocation;
        employee.setUtilisationPct(newUtilPct);
        internalEmployeeRepository.save(employee);

        // 9. Async Kafka Publish (NOT IMPLEMENTED NOW)
        // TODO: After DB transaction commits, publish BaseEvent<MatchNominatedEvent>
        //       with eventType="match.nominated" to topic "internal-match-events".
        //       Keep publish async / fire-and-forget so HTTP response is not blocked.

        // 10. Return NominationResponse
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
                .utilisationAfter(newUtilPct)
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

    private boolean isDemandAlreadyFulfilledInternally(DemandDto demand) {
        int recruitedCount = safeInt(demand.getRecruitedCount());
        int internalFilledCount = safeInt(demand.getInternalFilledCount());
        return recruitedCount > 0 && recruitedCount == internalFilledCount;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
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
                .build();
    }
}
