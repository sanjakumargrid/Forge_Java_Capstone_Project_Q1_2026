package com.talentgrid.workforce.rmgsearch.dto;

import com.talentgrid.workforce.engineerprofilemanagement.enums.ContractType;
import com.talentgrid.workforce.engineerprofilemanagement.enums.Level;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RmgSearchRequest {

    /** Case-insensitive substring match against employee code or internal id. */
    private String employeeId;

    /** Case-insensitive substring match against the employee's name. */
    private String name;

    /** Case-insensitive substring match against the employee's email. */
    private String email;

    /** Exact skill match (case-insensitive, OR logic) against the employee's skills array. */
    private List<String> skills;

    /** Earliest availability date to include (inclusive). */
    private LocalDate availabilityDateFrom;

    /** Latest availability date to include (inclusive). */
    private LocalDate availabilityDateTo;

    /** Case-insensitive substring match against the employee's location. */
    private String location;

    /** Seniority level — maps to the {@link Level} enum (SENIOR, MID, JUNIOR). */
    private Level level;

    /** Alias for {@link #level}; honoured only when {@code level} is not set. */
    private Level seniority;

    /** Employment contract type — FULL_TIME, CONTRACT, or PART_TIME. */
    private ContractType contractType;
}
