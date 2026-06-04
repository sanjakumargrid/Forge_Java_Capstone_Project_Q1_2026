package com.talentgrid.demand.domain.statemachine;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.enums.ClosureReason;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.exception.IllegalDemandTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TransitionValidator}.
 *
 * Tests all four enforcement rules:
 *  1. Matrix check — illegal transitions throw.
 *  2. RMG 5-business-day gate — INTERNAL_SEARCH → OPEN_EXTERNAL blocked < 5 days.
 *  3. Closure reason enforcement — mismatched or missing reasons throw.
 *  4. ON_HOLD resume guard — target must match previousStatus.
 */
@DisplayName("TransitionValidator — Business Rule Tests")
class TransitionValidatorTest {

    private TransitionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TransitionValidator(new DemandStateMachine());
    }

    // ─── Helper ─────────────────────────────────────────────────────────────────

    private Demand demandWithStatus(DemandStatus status) {
        Demand d = new Demand();
        d.setStatus(status);
        return d;
    }

    // ─── 1. Matrix check ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Rule 1: Matrix check")
    class MatrixCheck {

        @Test
        @DisplayName("Legal transition DRAFT → PENDING_APPROVAL does not throw")
        void legal_draft_to_pendingApproval() {
            Demand d = demandWithStatus(DemandStatus.DRAFT);
            assertThatCode(() -> validator.validate(d, DemandStatus.PENDING_APPROVAL, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Illegal transition DRAFT → APPROVED throws IllegalDemandTransitionException")
        void illegal_draft_to_approved() {
            Demand d = demandWithStatus(DemandStatus.DRAFT);
            assertThatThrownBy(() -> validator.validate(d, DemandStatus.APPROVED, null))
                    .isInstanceOf(IllegalDemandTransitionException.class)
                    .hasMessageContaining("DRAFT")
                    .hasMessageContaining("APPROVED");
        }

        @Test
        @DisplayName("Illegal transition CLOSED → DRAFT throws (terminal state)")
        void illegal_closed_to_draft() {
            Demand d = demandWithStatus(DemandStatus.CLOSED);
            assertThatThrownBy(() -> validator.validate(d, DemandStatus.DRAFT, null))
                    .isInstanceOf(IllegalDemandTransitionException.class);
        }

        @Test
        @DisplayName("Illegal transition APPROVED → CANCELLED throws (must go via INTERNAL_SEARCH)")
        void illegal_approved_to_cancelled() {
            Demand d = demandWithStatus(DemandStatus.APPROVED);
            assertThatThrownBy(() -> validator.validate(d, DemandStatus.CANCELLED, null))
                    .isInstanceOf(IllegalDemandTransitionException.class);
        }
    }

    // ─── 2. RMG 5-business-day gate ─────────────────────────────────────────────

    @Nested
    @DisplayName("Rule 2: RMG 5-business-day gate (INTERNAL_SEARCH → OPEN_EXTERNAL)")
    class RmgFiveDayGate {

        @Test
        @DisplayName("Throws when searchStartAt is null")
        void throws_when_searchStartAt_null() {
            Demand d = demandWithStatus(DemandStatus.INTERNAL_SEARCH);
            d.setSearchStartAt(null); // explicitly null

            assertThatThrownBy(() -> validator.validate(d, DemandStatus.OPEN_EXTERNAL, null))
                    .isInstanceOf(IllegalDemandTransitionException.class)
                    .hasMessageContaining("searchStartAt");
        }

        @Test
        @DisplayName("Throws when only 2 business days have elapsed")
        void throws_when_only_2_business_days_elapsed() {
            Demand d = demandWithStatus(DemandStatus.INTERNAL_SEARCH);
            // 2 business days ago: set to 2 weekdays back
            d.setSearchStartAt(OffsetDateTime.now().minusDays(2));

            assertThatThrownBy(() -> validator.validate(d, DemandStatus.OPEN_EXTERNAL, null))
                    .isInstanceOf(IllegalDemandTransitionException.class)
                    .hasMessageContaining("5 business days");
        }

        @Test
        @DisplayName("Throws when 4 business days have elapsed (still under gate)")
        void throws_when_4_business_days_elapsed() {
            Demand d = demandWithStatus(DemandStatus.INTERNAL_SEARCH);
            d.setSearchStartAt(OffsetDateTime.now().minusDays(4));

            assertThatThrownBy(() -> validator.validate(d, DemandStatus.OPEN_EXTERNAL, null))
                    .isInstanceOf(IllegalDemandTransitionException.class);
        }

        @Test
        @DisplayName("Passes when 7 calendar days (5+ business days) have elapsed")
        void passes_when_7_calendar_days_elapsed() {
            Demand d = demandWithStatus(DemandStatus.INTERNAL_SEARCH);
            // 7 calendar days covers at least 5 business days regardless of weekends
            d.setSearchStartAt(OffsetDateTime.now().minusDays(7));

            // no closure reason needed for OPEN_EXTERNAL; rule 3 won't fire
            assertThatCode(() -> validator.validate(d, DemandStatus.OPEN_EXTERNAL, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Gate does NOT apply to INTERNAL_SEARCH → FILLED_INTERNAL")
        void gate_does_not_apply_to_filledInternal() {
            Demand d = demandWithStatus(DemandStatus.INTERNAL_SEARCH);
            d.setSearchStartAt(OffsetDateTime.now()); // started today

            // FILLED_INTERNAL requires ClosureReason.FILLED_INTERNAL
            assertThatCode(() -> validator.validate(d, DemandStatus.FILLED_INTERNAL, ClosureReason.FILLED_INTERNAL))
                    .doesNotThrowAnyException();
        }
    }

    // ─── 3. Closure reason enforcement ──────────────────────────────────────────

    @Nested
    @DisplayName("Rule 3: Closure reason enforcement")
    class ClosureReasonEnforcement {

        @Test
        @DisplayName("FILLED_INTERNAL without reason throws")
        void filledInternal_requires_reason() {
            Demand d = demandWithStatus(DemandStatus.INTERNAL_SEARCH);
            d.setSearchStartAt(OffsetDateTime.now()); // gate doesn't apply for this target

            assertThatThrownBy(() -> validator.validate(d, DemandStatus.FILLED_INTERNAL, null))
                    .isInstanceOf(IllegalDemandTransitionException.class)
                    .hasMessageContaining("FILLED_INTERNAL");
        }

        @Test
        @DisplayName("FILLED_INTERNAL with wrong reason throws")
        void filledInternal_with_wrong_reason_throws() {
            Demand d = demandWithStatus(DemandStatus.INTERNAL_SEARCH);
            d.setSearchStartAt(OffsetDateTime.now());

            assertThatThrownBy(() -> validator.validate(d, DemandStatus.FILLED_INTERNAL, ClosureReason.CANCELLED))
                    .isInstanceOf(IllegalDemandTransitionException.class)
                    .hasMessageContaining("CANCELLED"); // wrong reason mentioned in message
        }

        @Test
        @DisplayName("FILLED_INTERNAL with correct reason passes")
        void filledInternal_with_correct_reason_passes() {
            Demand d = demandWithStatus(DemandStatus.INTERNAL_SEARCH);
            d.setSearchStartAt(OffsetDateTime.now());

            assertThatCode(() -> validator.validate(d, DemandStatus.FILLED_INTERNAL, ClosureReason.FILLED_INTERNAL))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("CANCELLED requires ClosureReason.CANCELLED")
        void cancelled_requires_cancelled_reason() {
            Demand d = demandWithStatus(DemandStatus.INTERNAL_SEARCH);
            d.setSearchStartAt(OffsetDateTime.now());

            assertThatThrownBy(() -> validator.validate(d, DemandStatus.CANCELLED, null))
                    .isInstanceOf(IllegalDemandTransitionException.class);

            assertThatCode(() -> validator.validate(d, DemandStatus.CANCELLED, ClosureReason.CANCELLED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ON_HOLD requires ClosureReason.ON_HOLD")
        void onHold_requires_on_hold_reason() {
            Demand d = demandWithStatus(DemandStatus.INTERNAL_SEARCH);
            d.setSearchStartAt(OffsetDateTime.now());

            assertThatThrownBy(() -> validator.validate(d, DemandStatus.ON_HOLD, ClosureReason.CANCELLED))
                    .isInstanceOf(IllegalDemandTransitionException.class);

            assertThatCode(() -> validator.validate(d, DemandStatus.ON_HOLD, ClosureReason.ON_HOLD))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("DUPLICATE requires ClosureReason.DUPLICATE")
        void duplicate_requires_duplicate_reason() {
            Demand d = demandWithStatus(DemandStatus.PENDING_APPROVAL);

            assertThatThrownBy(() -> validator.validate(d, DemandStatus.DUPLICATE, null))
                    .isInstanceOf(IllegalDemandTransitionException.class);

            assertThatCode(() -> validator.validate(d, DemandStatus.DUPLICATE, ClosureReason.DUPLICATE))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Reason=DUPLICATE with target=CANCELLED throws (mismatch)")
        void mismatch_duplicate_reason_for_cancelled_target_throws() {
            Demand d = demandWithStatus(DemandStatus.INTERNAL_SEARCH);
            d.setSearchStartAt(OffsetDateTime.now());

            assertThatThrownBy(() -> validator.validate(d, DemandStatus.CANCELLED, ClosureReason.DUPLICATE))
                    .isInstanceOf(IllegalDemandTransitionException.class);
        }

        @Test
        @DisplayName("States with no required reason accept null (e.g. PENDING_APPROVAL → APPROVED)")
        void no_reason_required_for_approved() {
            Demand d = demandWithStatus(DemandStatus.PENDING_APPROVAL);

            assertThatCode(() -> validator.validate(d, DemandStatus.APPROVED, null))
                    .doesNotThrowAnyException();
        }
    }

    // ─── 4. ON_HOLD resume guard ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Rule 4: ON_HOLD resume guard")
    class OnHoldResumeGuard {

        @Test
        @DisplayName("Resume to INTERNAL_SEARCH when previousStatus=INTERNAL_SEARCH passes")
        void resume_to_internalSearch_when_previous_was_internalSearch() {
            Demand d = demandWithStatus(DemandStatus.ON_HOLD);
            d.setPreviousStatus(DemandStatus.INTERNAL_SEARCH);

            assertThatCode(() -> validator.validate(d, DemandStatus.INTERNAL_SEARCH, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Resume to OPEN_EXTERNAL when previousStatus=OPEN_EXTERNAL passes")
        void resume_to_openExternal_when_previous_was_openExternal() {
            Demand d = demandWithStatus(DemandStatus.ON_HOLD);
            d.setPreviousStatus(DemandStatus.OPEN_EXTERNAL);

            assertThatCode(() -> validator.validate(d, DemandStatus.OPEN_EXTERNAL, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Resume to OPEN_EXTERNAL when previousStatus=INTERNAL_SEARCH throws (illegal jump)")
        void resume_to_openExternal_when_previous_was_internalSearch_throws() {
            Demand d = demandWithStatus(DemandStatus.ON_HOLD);
            d.setPreviousStatus(DemandStatus.INTERNAL_SEARCH);

            assertThatThrownBy(() -> validator.validate(d, DemandStatus.OPEN_EXTERNAL, null))
                    .isInstanceOf(IllegalDemandTransitionException.class)
                    .hasMessageContaining("INTERNAL_SEARCH");
        }

        @Test
        @DisplayName("Resume to INTERNAL_SEARCH when previousStatus=OPEN_EXTERNAL throws (illegal jump)")
        void resume_to_internalSearch_when_previous_was_openExternal_throws() {
            Demand d = demandWithStatus(DemandStatus.ON_HOLD);
            d.setPreviousStatus(DemandStatus.OPEN_EXTERNAL);

            assertThatThrownBy(() -> validator.validate(d, DemandStatus.INTERNAL_SEARCH, null))
                    .isInstanceOf(IllegalDemandTransitionException.class)
                    .hasMessageContaining("OPEN_EXTERNAL");
        }

        @Test
        @DisplayName("Throws when previousStatus is null on resume attempt")
        void resume_when_previousStatus_null_throws() {
            Demand d = demandWithStatus(DemandStatus.ON_HOLD);
            d.setPreviousStatus(null);

            assertThatThrownBy(() -> validator.validate(d, DemandStatus.INTERNAL_SEARCH, null))
                    .isInstanceOf(IllegalDemandTransitionException.class)
                    .hasMessageContaining("previousStatus");
        }

        @Test
        @DisplayName("ON_HOLD → CANCELLED (non-resume) does not trigger resume guard")
        void onHold_to_cancelled_does_not_trigger_resume_guard() {
            Demand d = demandWithStatus(DemandStatus.ON_HOLD);
            d.setPreviousStatus(DemandStatus.INTERNAL_SEARCH); // irrelevant for cancel

            assertThatCode(() -> validator.validate(d, DemandStatus.CANCELLED, ClosureReason.CANCELLED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ON_HOLD → CLOSED does not trigger resume guard")
        void onHold_to_closed_does_not_trigger_resume_guard() {
            Demand d = demandWithStatus(DemandStatus.ON_HOLD);
            d.setPreviousStatus(DemandStatus.OPEN_EXTERNAL);

            assertThatCode(() -> validator.validate(d, DemandStatus.CLOSED, null))
                    .doesNotThrowAnyException();
        }
    }
}
