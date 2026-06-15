package com.talentgrid.workforce.aiupskill.controller;

import com.talentgrid.workforce.aiupskill.dto.EmployeeAuditRequest;
import com.talentgrid.workforce.aiupskill.dto.UpskillingRecommendationResponse;
import com.talentgrid.workforce.aiupskill.entity.UpskillHistoryEntity;
import com.talentgrid.workforce.aiupskill.service.UpskillOrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce/ai/upskilling/recommendations")
public class UpskillOrchestrationController {

    private final UpskillOrchestrationService orchestrationService;

    public UpskillOrchestrationController(UpskillOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    /**
     * Generates a personalized AI-driven upskilling path for a specific employee.
     * Only employeeId is required — the algorithm internally compares the employee
     * against ALL open demands to compute and rank the top skill gaps.
     */
    @PostMapping("/generate")
    public ResponseEntity<UpskillingRecommendationResponse> generateLearningPath(
            @RequestBody EmployeeAuditRequest request) {
        UpskillingRecommendationResponse response = orchestrationService
                .generateAndLogUpskillingPath(request.employeeId());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the complete audit trail of upskilling recommendations generated
     * for an employee.
     * Records are returned sorted by generation timestamp, newest first.
     */
    @GetMapping("/history")
    public ResponseEntity<List<UpskillHistoryEntity>> getHistory(@RequestParam String employeeId) {
        List<UpskillHistoryEntity> historyLog = orchestrationService.getEmployeeAuditHistory(employeeId);
        return ResponseEntity.ok(historyLog);
    }
}
