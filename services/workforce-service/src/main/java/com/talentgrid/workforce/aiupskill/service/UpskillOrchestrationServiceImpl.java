package com.talentgrid.workforce.aiupskill.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.workforce.aiupskill.client.AiUpskillingClient;
import com.talentgrid.workforce.aiupskill.client.Team1DemandClient;
import com.talentgrid.workforce.aiupskill.client.Team5EngineerClient;
import com.talentgrid.workforce.aiupskill.dto.DemandPageResponse;
import com.talentgrid.workforce.aiupskill.dto.MissingSkillsRequest;
import com.talentgrid.workforce.aiupskill.dto.Team1DemandApiDto;
import com.talentgrid.workforce.aiupskill.dto.Team5EngineerApiDto;
import com.talentgrid.workforce.aiupskill.dto.UpskillingRecommendationResponse;
import com.talentgrid.workforce.aiupskill.entity.UpskillHistoryEntity;
import com.talentgrid.workforce.aiupskill.repository.UpskillHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UpskillOrchestrationServiceImpl implements UpskillOrchestrationService {

    private static final String PERSONALIZED_DEMAND_ID = "ALL-OPEN-DEMANDS";
    private static final int    TOP_SKILL_LIMIT        = 5;

    private final AiUpskillingClient aiClient;
    private final Team1DemandClient team1DemandClient;
    private final Team5EngineerClient team5EngineerClient;
    private final UpskillHistoryRepository historyRepository;
    private final ObjectMapper             objectMapper;

    public UpskillOrchestrationServiceImpl(AiUpskillingClient aiClient,
                                           Team1DemandClient team1DemandClient,
                                           Team5EngineerClient team5EngineerClient,
                                           UpskillHistoryRepository historyRepository,
                                           ObjectMapper objectMapper) {
        this.aiClient            = aiClient;
        this.team1DemandClient   = team1DemandClient;
        this.team5EngineerClient = team5EngineerClient;
        this.historyRepository   = historyRepository;
        this.objectMapper        = objectMapper;
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
        DemandPageResponse pageResponse = team1DemandClient.getDemandsPage(500);
        List<Team1DemandApiDto> allDemands = pageResponse != null && pageResponse.content() != null
                ? pageResponse.content()
                : Collections.emptyList();

        Set<String> openStatuses = Set.of("INTERNAL_SEARCH", "OPEN_EXTERNAL", "FILLED_PARTIALLY", "OPEN");

        List<Team1DemandApiDto> openDemands = allDemands.stream()
                .filter(demand -> demand.status() != null && openStatuses.contains(demand.status().toUpperCase()))
                .map(d -> {
                    try {
                        return team1DemandClient.getDemandById(d.demandId());
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Step B: Fetch the engineer profile directly by employeeId
        Team5EngineerApiDto targetEngineer = team5EngineerClient.getEngineerById(employeeId);
        if (targetEngineer == null) {
            throw new RuntimeException("Employee profile not found for ID: " + employeeId);
        }

        Set<String> engineerSkills = new HashSet<>(
                targetEngineer.skills() != null ? targetEngineer.skills() : Collections.emptyList());

        // Step C & D & E: Cross-compare this single employee against ALL open demands.
        // Aggregate missing skill frequencies: value = number of open demands requiring that skill.
        Map<String, Integer> skillGapFrequency = new HashMap<>();

        for (Team1DemandApiDto demand : openDemands) {
            Set<String> demandedSkills = new HashSet<>(
                    demand.skills() != null ? demand.skills() : Collections.emptyList());

            // Missing skills = Demand Skills − Employee Skills
            Set<String> missingSkills = new HashSet<>(demandedSkills);
            missingSkills.removeAll(engineerSkills);

            // Each gap increments the frequency counter for that skill
            for (String skill : missingSkills) {
                skillGapFrequency.merge(skill, 1, Integer::sum);
            }
        }

        // Step F: Sort descending by frequency — take the top 3 to 5 most critical gaps
        List<String> topMissingSkills = skillGapFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(TOP_SKILL_LIMIT)
                .map(Map.Entry::getKey)
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