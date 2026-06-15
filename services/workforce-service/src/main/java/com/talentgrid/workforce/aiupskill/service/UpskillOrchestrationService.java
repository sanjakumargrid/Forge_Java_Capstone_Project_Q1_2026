package com.talentgrid.workforce.aiupskill.service;


import com.talentgrid.workforce.aiupskill.dto.UpskillingRecommendationResponse;
import com.talentgrid.workforce.aiupskill.entity.UpskillHistoryEntity;

import java.util.List;

public interface UpskillOrchestrationService {
    UpskillingRecommendationResponse generateAndLogUpskillingPath(String employeeId);
    List<UpskillHistoryEntity> getEmployeeAuditHistory(String employeeId);
}