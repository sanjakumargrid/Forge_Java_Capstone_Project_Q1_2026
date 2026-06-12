package com.talentgrid.demand.domain.enums;

/**
 * Workforce seniority levels mapped to T-level grades.
 *
 * <p>
 * The T-level (T1–T7) is a standardized grade used across TalentGrid
 * for compensation bands, role expectations, and demand matching.
 *
 * <p>
 * Stored as enum name in the database (e.g., {@code "T3_SENIOR"}).
 */
public enum SeniorityLevel {
    IC0("IC0", "Intern"),
    T1_INTERN("T1", "Junior Engineer"),
    T2_JUNIOR("T2", "Junior Engineer"),
    T3_MID("T3", "Mid-Level Engineer"),
    T4_SENIOR("T4", "Senior Engineer"),
    T5_LEAD("T5", "Lead / Staff Engineer"),
    T6_PRINCIPAL("T6", "Principal Engineer"),
    T7_ARCHITECT("T7", "Distinguished / Architect");

    private final String grade;
    private final String displayName;

    SeniorityLevel(String grade, String displayName) {
        this.grade = grade;
        this.displayName = displayName;
    }

    /** Returns the T-level grade code (e.g., "T4"). */
    public String getGrade() {
        return grade;
    }

    /** Returns the human-readable title (e.g., "Senior Engineer"). */
    public String getDisplayName() {
        return displayName;
    }
}
