package com.talentgrid.demand.domain.enums;

/**
 * Reasons for closing or transitioning a demand.
 * Each reason code maps to one or more valid target states per the transition matrix.
 *
 * <ul>
 *   <li>{@code FILLED_INTERNAL}  — Valid for target states: FILLED_INTERNAL, CLOSED (from FILLED_PARTIALLY)</li>
 *   <li>{@code FILLED_EXTERNAL}  — Valid for target states: FILLED_EXTERNAL, CLOSED (from FILLED_PARTIALLY)</li>
 *   <li>{@code CANCELLED}        — Valid for target state:  CANCELLED</li>
 *   <li>{@code ON_HOLD}          — Valid for target state:  ON_HOLD</li>
 *   <li>{@code DUPLICATE}        — Valid for target state:  DUPLICATE</li>
 * </ul>
 */
public enum ClosureReason {
    FILLED_INTERNAL,
    FILLED_EXTERNAL,
    CANCELLED,
    ON_HOLD,
    DUPLICATE
}
