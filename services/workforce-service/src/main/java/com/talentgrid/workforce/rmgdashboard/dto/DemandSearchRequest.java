package com.talentgrid.workforce.rmgdashboard.dto;

import com.talentgrid.workforce.engineerprofilemanagement.enums.ContractType;
import com.talentgrid.workforce.engineerprofilemanagement.enums.Level;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DemandSearchRequest {

    /** Exact match on demand ID. */
    private Long demandId;

    /** Case-insensitive substring match against the demand title. */
    private String title;

    /** Exact skill match (case-insensitive) against mandatory or optional skills. */
    private String skill;

    /** Case-insensitive substring match against the demand location. */
    private String location;

    /** Seniority level — maps to the {@link Level} enum (SENIOR, MID, JUNIOR). */
    private Level level;

    /** Single status filter (backward compatible with {@code /demands?status=}). */
    private String status;

    /** Multiple status filter — repeat {@code statuses} query param or comma-separated. */
    private List<String> statuses;

    /** Demand priority — LOW, MEDIUM, HIGH, or CRITICAL. */
    private String priority;

    /** Case-insensitive substring match against the client account name. */
    private String accountName;

    /** Case-insensitive exact match against the business unit. */
    private String businessUnit;

    /** Employment contract type — FULL_TIME, CONTRACT, or PART_TIME. */
    private ContractType employmentType;

    /** Filter by assigned resource manager user ID. */
    private Long assignedRm;

    /** Earliest target fill date to include (inclusive). */
    private LocalDate targetDateFrom;

    /** Latest target fill date to include (inclusive). */
    private LocalDate targetDateTo;
}
