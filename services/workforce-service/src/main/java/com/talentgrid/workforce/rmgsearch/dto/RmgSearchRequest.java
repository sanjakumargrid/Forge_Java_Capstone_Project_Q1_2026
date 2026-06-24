package com.talentgrid.workforce.rmgsearch.dto;

import com.talentgrid.workforce.engineerprofilemanagement.enums.ContractType;
import com.talentgrid.workforce.engineerprofilemanagement.enums.Level;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RmgSearchRequest {

    /** Exact skill match (case-insensitive) against the employee's skills array. */
    private String skill;

    /** Earliest availability date to include (inclusive). */
    private LocalDate availabilityDateFrom;

    /** Latest availability date to include (inclusive). */
    private LocalDate availabilityDateTo;

    /** Case-insensitive substring match against the employee's location. */
    private String location;

    /** Seniority level — maps to the {@link Level} enum (SENIOR, MID, JUNIOR). */
    private Level seniority;

    /** Employment contract type — FULL_TIME, CONTRACT, or PART_TIME. */
    private ContractType contractType;
}
