package com.talentgrid.workforce.aiupskill.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.workforce.aiupskill.client.AiUpskillingClient;
import com.talentgrid.workforce.aiupskill.client.DemandClient;
import com.talentgrid.workforce.aiupskill.dto.DemandPageResponse;
import com.talentgrid.workforce.aiupskill.dto.MissingSkillsRequest;
import com.talentgrid.workforce.aiupskill.dto.DemandApiDto;
import com.talentgrid.workforce.aiupskill.dto.UpskillingRecommendationResponse;
import com.talentgrid.workforce.aiupskill.entity.UpskillHistoryEntity;
import com.talentgrid.workforce.aiupskill.repository.UpskillHistoryRepository;
import com.talentgrid.workforce.engineerprofilemanagement.dto.InternalEmployeeResponse;
import com.talentgrid.workforce.engineerprofilemanagement.service.InternalEmployeeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UpskillOrchestrationServiceImpl implements UpskillOrchestrationService {

    private static final String PERSONALIZED_DEMAND_ID = "ALL-OPEN-DEMANDS";
    private static final int    TOP_SKILL_LIMIT        = 5;

    private final AiUpskillingClient aiClient;
    private final DemandClient demandClient;
    private final InternalEmployeeService internalEmployeeService;
    private final UpskillHistoryRepository historyRepository;
    private final ObjectMapper             objectMapper;

    public UpskillOrchestrationServiceImpl(AiUpskillingClient aiClient,
                                           DemandClient demandClient,
                                           InternalEmployeeService internalEmployeeService,
                                           UpskillHistoryRepository historyRepository,
                                           ObjectMapper objectMapper) {
        this.aiClient                = aiClient;
        this.demandClient = demandClient;
        this.internalEmployeeService = internalEmployeeService;
        this.historyRepository       = historyRepository;
        this.objectMapper            = objectMapper;
    }

    /**
     * Personalized Upskilling Engine — Single Employee vs. All Open Demands.
     *
     * Given only an employeeId this method:
     *   A) Fetches all OPEN demands from Team 1
     *   B) Locates the employee's exact skill profile from Team 5
     *   C-E) Cross-compares against every open demand, tallying missing skill frequencies
     *   F) Ranks the top 5 most in-demand missing skills
     *   G) Dispatches them to the AI service for course recommendations
     *   H) Persists the result under demand_id = "ALL-OPEN-DEMANDS" and returns the response
     */
    @Override
    public UpskillingRecommendationResponse generateAndLogUpskillingPath(String employeeId) {

        // Step A: Fetch all demands from Team 1 — filter to keep only OPEN status
        DemandPageResponse pageResponse = demandClient.getDemandsPage(500);
        List<DemandApiDto> allDemands = pageResponse != null && pageResponse.content() != null
                ? pageResponse.content()
                : Collections.emptyList();

        Set<String> openStatuses = Set.of("INTERNAL_SEARCH", "OPEN_EXTERNAL", "FILLED_PARTIALLY", "OPEN");

        List<DemandApiDto> openDemands = allDemands.stream()
                .filter(demand -> demand.status() != null && openStatuses.contains(demand.status().toUpperCase()))
                .map(d -> {
                    try {
                        return demandClient.getDemandById(d.demandId());
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Step B: Fetch the engineer profile directly by employeeId or email
        InternalEmployeeResponse targetEmployee;
        if (employeeId != null && employeeId.contains("@")) {
            targetEmployee = internalEmployeeService.getEmployeeByEmail(employeeId);
        } else {
            Long empIdLong;
            try {
                empIdLong = Long.valueOf(employeeId);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid employee ID format: " + employeeId);
            }
            targetEmployee = internalEmployeeService.getEmployeeDetailsById(empIdLong);
        }

        if (targetEmployee == null) {
            throw new RuntimeException("Employee profile not found for ID/Email: " + employeeId);
        }

        Set<String> engineerSkillsLower = targetEmployee.getSkills() != null
                ? Arrays.stream(targetEmployee.getSkills())
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        // Step C & D & E: Cross-compare this single employee against ALL open demands.
        // Aggregate missing skill frequencies: value = number of open demands requiring that skill.
        Map<String, Integer> skillGapFrequencyLower = new HashMap<>();
        Map<String, String> lowerToOriginalCase = new HashMap<>();

        for (DemandApiDto demand : openDemands) {
            List<String> demandSkills = demand.skills();
            if (demandSkills == null) {
                continue;
            }

            for (String skill : demandSkills) {
                if (skill == null || skill.trim().isEmpty()) {
                    continue;
                }
                String skillTrimmed = skill.trim();
                String skillLower = skillTrimmed.toLowerCase();
                if (!engineerSkillsLower.contains(skillLower)) {
                    skillGapFrequencyLower.merge(skillLower, 1, Integer::sum);
                    lowerToOriginalCase.putIfAbsent(skillLower, skillTrimmed);
                }
            }
        }

        // Step F: Sort descending by frequency — take the top 3 to 5 most critical gaps
        List<String> topMissingSkills = skillGapFrequencyLower.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(TOP_SKILL_LIMIT)
                .map(entry -> lowerToOriginalCase.get(entry.getKey()))
                .collect(Collectors.toList());

        // Guard: Employee already possesses all skills required by all open demands
        if (topMissingSkills.isEmpty()) {
            return new UpskillingRecommendationResponse(Collections.emptyList());
        }

        // Step G: Send the top missing skills to the AI service for course recommendations
        UpskillingRecommendationResponse aiResponse = aiClient.getRecommendations(
                new MissingSkillsRequest(topMissingSkills));

        // Step H: Serialize and persist — demand_id is always "ALL-OPEN-DEMANDS"
        try {
            String jsonPayload = objectMapper.writeValueAsString(aiResponse);

            UpskillHistoryEntity auditLog = new UpskillHistoryEntity();
            auditLog.setEmployeeId(employeeId);
            auditLog.setDemandId(PERSONALIZED_DEMAND_ID);
            auditLog.setGeneratedAt(LocalDateTime.now());
            auditLog.setRawPayloadJson(jsonPayload);

            historyRepository.save(auditLog);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize personalized learning path for audit tracking", e);
        }

        return aiResponse;
    }

    @Override
    public List<UpskillHistoryEntity> getEmployeeAuditHistory(String employeeId) {
        return historyRepository.findByEmployeeIdOrderByGeneratedAtDesc(employeeId);
    }
}