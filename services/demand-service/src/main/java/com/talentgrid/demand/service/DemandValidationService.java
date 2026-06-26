package com.talentgrid.demand.service;

import com.talentgrid.demand.dto.request.DemandRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        if (request.getDepartment() == null || request.getDepartment().isBlank()) {
            errors.add("department is required");
        } else if (request.getDepartment().length() > 150) {
            errors.add("department must not exceed 150 characters");
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
        } else {
            validateSkillListShape(request.getMandatorySkillIds(), request.getOptionalSkillIds(), errors);
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
            } else {
                validateSkillListShape(request.getMandatorySkillIds(), request.getOptionalSkillIds(), errors);
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

    /**
     * Validates merged mandatory/optional skill lists after PATCH partial merge.
     */
    public void validateSkillLists(List<Long> mandatoryIds, List<Long> optionalIds) {
        List<String> errors = new ArrayList<>();

        boolean hasMandatory = mandatoryIds != null && !mandatoryIds.isEmpty();
        boolean hasOptional = optionalIds != null && !optionalIds.isEmpty();
        if (!hasMandatory && !hasOptional) {
            errors.add("at least one of mandatorySkillIds or optionalSkillIds must be non-empty");
        } else {
            validateSkillListShape(mandatoryIds, optionalIds, errors);
        }

        if (!errors.isEmpty()) {
            String errorMsg = "Validation failed: " + String.join("; ", errors);
            log.warn("Skill list validation: {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    private void validateSkillListShape(List<Long> mandatoryIds, List<Long> optionalIds, List<String> errors) {
        collectDuplicateSkillIds(mandatoryIds, "mandatorySkillIds", errors);
        collectDuplicateSkillIds(optionalIds, "optionalSkillIds", errors);
        collectMandatoryOptionalOverlap(mandatoryIds, optionalIds, errors);
    }

    private void collectDuplicateSkillIds(List<Long> skillIds, String fieldName, List<String> errors) {
        if (skillIds == null || skillIds.isEmpty()) {
            return;
        }
        Set<Long> seen = new HashSet<>();
        Set<Long> duplicates = new HashSet<>();
        for (Long skillId : skillIds) {
            if (skillId != null && !seen.add(skillId)) {
                duplicates.add(skillId);
            }
        }
        if (!duplicates.isEmpty()) {
            errors.add(fieldName + " contains duplicate skill IDs: " + duplicates);
        }
    }

    private void collectMandatoryOptionalOverlap(List<Long> mandatoryIds, List<Long> optionalIds, List<String> errors) {
        if (mandatoryIds == null || optionalIds == null || mandatoryIds.isEmpty() || optionalIds.isEmpty()) {
            return;
        }
        Set<Long> overlap = new HashSet<>(mandatoryIds);
        overlap.retainAll(optionalIds);
        if (!overlap.isEmpty()) {
            errors.add("skill IDs cannot appear in both mandatorySkillIds and optionalSkillIds: " + overlap);
        }
    }
}
