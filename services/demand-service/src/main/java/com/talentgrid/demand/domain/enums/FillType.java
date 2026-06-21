package com.talentgrid.demand.domain.enums;

/**
 * Indicates how a demand was filled in the single-person model.
 *
 * <ul>
 *   <li>{@link #INTERNAL} — filled by matching an employee from the internal bench.</li>
 *   <li>{@link #EXTERNAL} — filled by an externally hired candidate.</li>
 * </ul>
 *
 * <p>Stored as {@code fill_type} on the {@code demands} table.
 * The value is {@code null} until the demand transitions to a filled state.
 */
public enum FillType {

    /** The demand was filled by an internal bench employee. */
    INTERNAL,

    /** The demand was filled by an external hire. */
    EXTERNAL
}
