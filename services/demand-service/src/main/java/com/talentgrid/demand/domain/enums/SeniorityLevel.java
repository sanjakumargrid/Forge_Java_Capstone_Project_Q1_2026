package com.talentgrid.demand.domain.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Workforce seniority levels mapped to standardized grade codes used across TalentGrid.
 *
 * <p>Two tracks are supported:
 * <ul>
 *   <li><b>Individual Contributor (IC / T-track)</b> — IC0, T0 through T7</li>
 *   <li><b>Delivery / Management (D-track)</b> — D0 through D6</li>
 * </ul>
 *
 * <p>During demand creation, callers supply only the {@link #grade} code (e.g., {@code "T4"}).
 * The enum constant and its {@link #displayName} are resolved internally via
 * {@link #fromGrade(String)}.
 *
 * <p>Stored as the enum constant name in the database (e.g., {@code "T4_STAFF"}) using
 * {@code @Enumerated(EnumType.STRING)}, so existing rows are unaffected by display-name changes.
 */
public enum SeniorityLevel {

    // ─── Individual Contributor / Engineering Track ───────────────────────────

    /** Intern (pre-T-track). */
    IC0("IC0", "Intern"),

    /** Engineer Intern. */
    T0_INTERN("T0", "Engineer Intern"),

    /** Junior Engineer. */
    T1_JUNIOR("T1", "Junior Engineer"),

    /** Mid-Level Engineer. */
    T2_MID("T2", "Mid-Level Engineer"),

    /** Senior Engineer. */
    T3_SENIOR("T3", "Senior Engineer"),

    /** Staff Engineer. */
    T4_STAFF("T4", "Staff Engineer"),

    /** Lead / Staff Engineer / Senior Staff Engineer. */
    T5_SENIOR_STAFF("T5", "Lead / Staff Engineer / Senior Staff Engineer"),

    /** Principal Engineer. */
    T6_PRINCIPAL("T6", "Principal Engineer"),

    /** Distinguished Engineer / Architect. */
    T7_DISTINGUISHED("T7", "Distinguished / Architect"),

    // ─── Delivery / Management Track ─────────────────────────────────────────

    /** Associate Delivery Manager / Technical Project Manager. */
    D0_ASSOCIATE("D0", "Associate Delivery Manager / Technical Project Manager"),

    /** Senior Associate Delivery Manager / Technical Project Manager. */
    D1_SENIOR_ASSOCIATE("D1", "Senior Associate Delivery Manager / Technical Project Manager"),

    /** Delivery Manager. */
    D2_MID("D2", "Delivery Manager"),

    /** Senior Delivery Manager. */
    D3_SENIOR("D3", "Senior Delivery Manager"),

    /** Principal Delivery Manager. */
    D4_PRINCIPAL("D4", "Principal Delivery Manager"),

    /** Delivery Director. */
    D5_DIRECTOR("D5", "Delivery Director"),

    /** Senior Delivery Director. */
    D6_SENIOR_DIRECTOR("D6", "Senior Delivery Director");

    // ─── Fields ───────────────────────────────────────────────────────────────

    /** Short grade code shown to the user during demand creation (e.g., {@code "T4"}). */
    private final String grade;

    /** Human-readable role title mapped to this grade in the employee system. */
    private final String displayName;

    // ─── Lookup cache ─────────────────────────────────────────────────────────

    private static final Map<String, SeniorityLevel> BY_GRADE =
            Arrays.stream(values())
                  .collect(Collectors.toUnmodifiableMap(SeniorityLevel::getGrade, e -> e));

    // ─── Constructor ──────────────────────────────────────────────────────────

    SeniorityLevel(String grade, String displayName) {
        this.grade = grade;
        this.displayName = displayName;
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    /**
     * Returns the short grade code used in demand creation and employee matching
     * (e.g., {@code "T4"}, {@code "D3"}).
     */
    public String getGrade() {
        return grade;
    }

    /**
     * Returns the human-readable role title as it appears in the employee system
     * (e.g., {@code "Staff Engineer"}, {@code "Senior Delivery Manager"}).
     */
    public String getDisplayName() {
        return displayName;
    }

    // ─── Static factory ───────────────────────────────────────────────────────

    /**
     * Looks up a {@code SeniorityLevel} by its grade code (case-sensitive).
     *
     * <p>Use this when deserializing a grade code received from an API request
     * or when matching an employee's {@code grade} attribute to a demand level.
     *
     * @param grade the grade code to look up (e.g., {@code "T4"}, {@code "D3"})
     * @return the matching {@code SeniorityLevel}
     * @throws IllegalArgumentException if no level is registered for the given grade
     */
    public static SeniorityLevel fromGrade(String grade) {
        SeniorityLevel level = BY_GRADE.get(grade);
        if (level == null) {
            throw new IllegalArgumentException(
                    "Unknown seniority grade: '" + grade + "'. " +
                    "Valid grades: " + BY_GRADE.keySet());
        }
        return level;
    }
}
