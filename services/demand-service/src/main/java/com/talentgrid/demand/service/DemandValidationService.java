package com.talentgrid.demand.service;

import com.talentgrid.demand.dto.request.DemandRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates demand request DTOs before persistence.
 * All fields are type-checked server-side as required by REQ-DM-01.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>{@code title} — required, max 255 characters</li>
 *   <li>{@code level} — required (seniority level)</li>
 *   <li>{@code priority} — required</li>
 *   <li>{@code budget} — if provided, must be &ge; 0</li>
 *   <li>{@code jobTitleId} — required</li>
 *   <li>{@code skills} — at least one of mandatorySkillIds or optionalSkillIds must be non-empty</li>
 * </ul>
 *
 * @throws IllegalArgumentException if any validation rule is violated
 */
@Service
@Slf4j
public class DemandValidationService {

    /**
     * Validates a create demand request. All mandatory fields must be present.
     *
     * @param request the create request to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateCreate(DemandRequest request) {
        List<String> errors = new ArrayList<>();
//
//        if (request.getTitle() == null || request.getTitle().isBlank()) {
//            errors.add("title is required");
//        } else if (request.getTitle().length() > 255) {
//            errors.add("title must not exceed 255 characters");
//        }

        if (request.getJobTitleId() == null) {
            errors.add("jobTitleId is required");
        }

        if (request.getLevel() == null) {
            errors.add("level (seniority level) is required");
        }

        if (request.getPriority() == null) {
            errors.add("priority is required");
        }

        if (request.getLocation() == null || request.getLocation().isBlank()) {
            errors.add("location is required");
        } else if (request.getLocation().length() > 150) {
            errors.add("location must not exceed 150 characters");
        }

        if (request.getProjectId() == null) {
            errors.add("projectId is required");
        }

        if (request.getBusinessUnit() == null || request.getBusinessUnit().isBlank()) {
            errors.add("businessUnit is required");
        } else if (request.getBusinessUnit().length() > 150) {
            errors.add("businessUnit must not exceed 150 characters");
        }

        if (request.getBudget() == null) {
            errors.add("budget is required");
        } else if (request.getBudget().signum() < 0) {
            errors.add("budget must be non-negative");
        }

        if (request.getTargetDate() == null) {
            errors.add("targetDate is required");
        }

        if (request.getJobTitleId() == null) {
            errors.add("jobTitleId is required");
        }

        boolean hasMandatory = request.getMandatorySkillIds() != null && !request.getMandatorySkillIds().isEmpty();
        boolean hasOptional = request.getOptionalSkillIds() != null && !request.getOptionalSkillIds().isEmpty();
        if (!hasMandatory && !hasOptional) {
            errors.add("at least one of mandatorySkillIds or optionalSkillIds must be non-empty");
        }

        if (!errors.isEmpty()) {
            String errorMsg = "Validation failed: " + String.join("; ", errors);
            log.warn("Create demand: {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * Validates an update demand request. Only non-null fields are validated.
     *
     * @param request the update request to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateUpdate(DemandRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.getTitle() != null) {
            if (request.getTitle().isBlank()) {
                errors.add("title must not be blank");
            } else if (request.getTitle().length() > 255) {
                errors.add("title must not exceed 255 characters");
            }
        }

        if (request.getBudget() != null && request.getBudget().signum() < 0) {
            errors.add("budget must be non-negative");
        }

        if (request.getMandatorySkillIds() != null || request.getOptionalSkillIds() != null) {
            boolean hasMandatory = request.getMandatorySkillIds() != null && !request.getMandatorySkillIds().isEmpty();
            boolean hasOptional = request.getOptionalSkillIds() != null && !request.getOptionalSkillIds().isEmpty();
            if (!hasMandatory && !hasOptional) {
                errors.add("at least one of mandatorySkillIds or optionalSkillIds must be non-empty when updating skills");
            }
        }

        if (request.getLocation() != null && request.getLocation().length() > 150) {
            errors.add("location must not exceed 150 characters");
        }

        if (request.getBusinessUnit() != null && request.getBusinessUnit().length() > 150) {
            errors.add("businessUnit must not exceed 150 characters");
        }

        if (!errors.isEmpty()) {
            String errorMsg = "Validation failed: " + String.join("; ", errors);
            log.warn("Update demand: {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }
}
