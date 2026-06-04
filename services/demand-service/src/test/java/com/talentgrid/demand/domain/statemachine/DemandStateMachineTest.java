package com.talentgrid.demand.domain.statemachine;

import com.talentgrid.demand.domain.enums.DemandStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DemandStateMachine}.
 *
 * Verifies:
 *  - Every transition in the full 12-state matrix returns true.
 *  - Every invalid/unlisted transition returns false.
 *  - CLOSED (terminal) has no outgoing transitions.
 *  - getAllowedTransitions() returns the correct sets.
 */
@DisplayName("DemandStateMachine — Transition Matrix Tests")
class DemandStateMachineTest {

    private DemandStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new DemandStateMachine();
    }

    // ─── Valid Transitions ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Valid transitions (must return true)")
    class ValidTransitions {

        @Test
        @DisplayName("DRAFT → PENDING_APPROVAL")
        void draft_to_pendingApproval() {
            assertThat(stateMachine.canTransition(DemandStatus.DRAFT, DemandStatus.PENDING_APPROVAL)).isTrue();
        }

        @Test
        @DisplayName("DRAFT → CANCELLED")
        void draft_to_cancelled() {
            assertThat(stateMachine.canTransition(DemandStatus.DRAFT, DemandStatus.CANCELLED)).isTrue();
        }

        @Test
        @DisplayName("PENDING_APPROVAL → APPROVED")
        void pendingApproval_to_approved() {
            assertThat(stateMachine.canTransition(DemandStatus.PENDING_APPROVAL, DemandStatus.APPROVED)).isTrue();
        }

        @Test
        @DisplayName("PENDING_APPROVAL → DRAFT (reject)")
        void pendingApproval_to_draft() {
            assertThat(stateMachine.canTransition(DemandStatus.PENDING_APPROVAL, DemandStatus.DRAFT)).isTrue();
        }

        @Test
        @DisplayName("PENDING_APPROVAL → DUPLICATE")
        void pendingApproval_to_duplicate() {
            assertThat(stateMachine.canTransition(DemandStatus.PENDING_APPROVAL, DemandStatus.DUPLICATE)).isTrue();
        }

        @Test
        @DisplayName("PENDING_APPROVAL → ON_HOLD")
        void pendingApproval_to_onHold() {
            assertThat(stateMachine.canTransition(DemandStatus.PENDING_APPROVAL, DemandStatus.ON_HOLD)).isTrue();
        }

        @Test
        @DisplayName("PENDING_APPROVAL → CANCELLED")
        void pendingApproval_to_cancelled() {
            assertThat(stateMachine.canTransition(DemandStatus.PENDING_APPROVAL, DemandStatus.CANCELLED)).isTrue();
        }

        @Test
        @DisplayName("APPROVED → INTERNAL_SEARCH (auto)")
        void approved_to_internalSearch() {
            assertThat(stateMachine.canTransition(DemandStatus.APPROVED, DemandStatus.INTERNAL_SEARCH)).isTrue();
        }

        @Test
        @DisplayName("INTERNAL_SEARCH → FILLED_INTERNAL")
        void internalSearch_to_filledInternal() {
            assertThat(stateMachine.canTransition(DemandStatus.INTERNAL_SEARCH, DemandStatus.FILLED_INTERNAL)).isTrue();
        }

        @Test
        @DisplayName("INTERNAL_SEARCH → FILLED_PARTIALLY")
        void internalSearch_to_filledPartially() {
            assertThat(stateMachine.canTransition(DemandStatus.INTERNAL_SEARCH, DemandStatus.FILLED_PARTIALLY)).isTrue();
        }

        @Test
        @DisplayName("INTERNAL_SEARCH → OPEN_EXTERNAL")
        void internalSearch_to_openExternal() {
            assertThat(stateMachine.canTransition(DemandStatus.INTERNAL_SEARCH, DemandStatus.OPEN_EXTERNAL)).isTrue();
        }

        @Test
        @DisplayName("INTERNAL_SEARCH → ON_HOLD")
        void internalSearch_to_onHold() {
            assertThat(stateMachine.canTransition(DemandStatus.INTERNAL_SEARCH, DemandStatus.ON_HOLD)).isTrue();
        }

        @Test
        @DisplayName("INTERNAL_SEARCH → CANCELLED")
        void internalSearch_to_cancelled() {
            assertThat(stateMachine.canTransition(DemandStatus.INTERNAL_SEARCH, DemandStatus.CANCELLED)).isTrue();
        }

        @Test
        @DisplayName("FILLED_PARTIALLY → OPEN_EXTERNAL")
        void filledPartially_to_openExternal() {
            assertThat(stateMachine.canTransition(DemandStatus.FILLED_PARTIALLY, DemandStatus.OPEN_EXTERNAL)).isTrue();
        }

        @Test
        @DisplayName("FILLED_PARTIALLY → CLOSED")
        void filledPartially_to_closed() {
            assertThat(stateMachine.canTransition(DemandStatus.FILLED_PARTIALLY, DemandStatus.CLOSED)).isTrue();
        }

        @Test
        @DisplayName("OPEN_EXTERNAL → FILLED_EXTERNAL")
        void openExternal_to_filledExternal() {
            assertThat(stateMachine.canTransition(DemandStatus.OPEN_EXTERNAL, DemandStatus.FILLED_EXTERNAL)).isTrue();
        }

        @Test
        @DisplayName("OPEN_EXTERNAL → ON_HOLD")
        void openExternal_to_onHold() {
            assertThat(stateMachine.canTransition(DemandStatus.OPEN_EXTERNAL, DemandStatus.ON_HOLD)).isTrue();
        }

        @Test
        @DisplayName("OPEN_EXTERNAL → CANCELLED")
        void openExternal_to_cancelled() {
            assertThat(stateMachine.canTransition(DemandStatus.OPEN_EXTERNAL, DemandStatus.CANCELLED)).isTrue();
        }

        @Test
        @DisplayName("ON_HOLD → INTERNAL_SEARCH (resume)")
        void onHold_to_internalSearch() {
            assertThat(stateMachine.canTransition(DemandStatus.ON_HOLD, DemandStatus.INTERNAL_SEARCH)).isTrue();
        }

        @Test
        @DisplayName("ON_HOLD → OPEN_EXTERNAL (resume)")
        void onHold_to_openExternal() {
            assertThat(stateMachine.canTransition(DemandStatus.ON_HOLD, DemandStatus.OPEN_EXTERNAL)).isTrue();
        }

        @Test
        @DisplayName("ON_HOLD → CANCELLED")
        void onHold_to_cancelled() {
            assertThat(stateMachine.canTransition(DemandStatus.ON_HOLD, DemandStatus.CANCELLED)).isTrue();
        }

        @Test
        @DisplayName("ON_HOLD → CLOSED")
        void onHold_to_closed() {
            assertThat(stateMachine.canTransition(DemandStatus.ON_HOLD, DemandStatus.CLOSED)).isTrue();
        }

        @Test
        @DisplayName("FILLED_INTERNAL → CLOSED")
        void filledInternal_to_closed() {
            assertThat(stateMachine.canTransition(DemandStatus.FILLED_INTERNAL, DemandStatus.CLOSED)).isTrue();
        }

        @Test
        @DisplayName("FILLED_EXTERNAL → CLOSED")
        void filledExternal_to_closed() {
            assertThat(stateMachine.canTransition(DemandStatus.FILLED_EXTERNAL, DemandStatus.CLOSED)).isTrue();
        }

        @Test
        @DisplayName("CANCELLED → CLOSED")
        void cancelled_to_closed() {
            assertThat(stateMachine.canTransition(DemandStatus.CANCELLED, DemandStatus.CLOSED)).isTrue();
        }

        @Test
        @DisplayName("DUPLICATE → CLOSED")
        void duplicate_to_closed() {
            assertThat(stateMachine.canTransition(DemandStatus.DUPLICATE, DemandStatus.CLOSED)).isTrue();
        }
    }

    // ─── Invalid Transitions ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Invalid transitions (must return false)")
    class InvalidTransitions {

        @Test
        @DisplayName("CLOSED is terminal — no transitions out")
        void closed_isTerminal() {
            for (DemandStatus target : DemandStatus.values()) {
                assertThat(stateMachine.canTransition(DemandStatus.CLOSED, target))
                        .as("CLOSED → %s should be false", target)
                        .isFalse();
            }
        }

        @Test
        @DisplayName("DRAFT → APPROVED (skipping PENDING_APPROVAL)")
        void draft_cannotSkipToApproved() {
            assertThat(stateMachine.canTransition(DemandStatus.DRAFT, DemandStatus.APPROVED)).isFalse();
        }

        @Test
        @DisplayName("DRAFT → INTERNAL_SEARCH (skipping approval)")
        void draft_cannotSkipToInternalSearch() {
            assertThat(stateMachine.canTransition(DemandStatus.DRAFT, DemandStatus.INTERNAL_SEARCH)).isFalse();
        }

        @Test
        @DisplayName("APPROVED → CANCELLED (must go through INTERNAL_SEARCH first)")
        void approved_cannotGoToCancelled() {
            assertThat(stateMachine.canTransition(DemandStatus.APPROVED, DemandStatus.CANCELLED)).isFalse();
        }

        @Test
        @DisplayName("INTERNAL_SEARCH → CLOSED (must fill or cancel first)")
        void internalSearch_cannotGoDirectlyToClosed() {
            assertThat(stateMachine.canTransition(DemandStatus.INTERNAL_SEARCH, DemandStatus.CLOSED)).isFalse();
        }

        @Test
        @DisplayName("FILLED_INTERNAL → OPEN_EXTERNAL (already filled internally)")
        void filledInternal_cannotOpenExternal() {
            assertThat(stateMachine.canTransition(DemandStatus.FILLED_INTERNAL, DemandStatus.OPEN_EXTERNAL)).isFalse();
        }

        @Test
        @DisplayName("CANCELLED → DRAFT (cannot un-cancel)")
        void cancelled_cannotReturnToDraft() {
            assertThat(stateMachine.canTransition(DemandStatus.CANCELLED, DemandStatus.DRAFT)).isFalse();
        }
    }

    // ─── getAllowedTransitions ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllowedTransitions()")
    class AllowedTransitions {

        @Test
        @DisplayName("CLOSED returns empty set")
        void closed_returnsEmptySet() {
            Set<DemandStatus> allowed = stateMachine.getAllowedTransitions(DemandStatus.CLOSED);
            assertThat(allowed).isEmpty();
        }

        @Test
        @DisplayName("DRAFT returns exactly 2 targets")
        void draft_returnsTwoTargets() {
            Set<DemandStatus> allowed = stateMachine.getAllowedTransitions(DemandStatus.DRAFT);
            assertThat(allowed).containsExactlyInAnyOrder(
                    DemandStatus.PENDING_APPROVAL,
                    DemandStatus.CANCELLED
            );
        }

        @Test
        @DisplayName("PENDING_APPROVAL returns exactly 5 targets")
        void pendingApproval_returnsFiveTargets() {
            Set<DemandStatus> allowed = stateMachine.getAllowedTransitions(DemandStatus.PENDING_APPROVAL);
            assertThat(allowed).containsExactlyInAnyOrder(
                    DemandStatus.APPROVED,
                    DemandStatus.DRAFT,
                    DemandStatus.DUPLICATE,
                    DemandStatus.ON_HOLD,
                    DemandStatus.CANCELLED
            );
        }
    }
}
