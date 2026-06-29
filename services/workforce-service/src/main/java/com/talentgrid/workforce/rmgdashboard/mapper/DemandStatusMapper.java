package com.talentgrid.workforce.rmgdashboard.mapper;

import java.util.Set;

/**
 * Translates RMG/UI-friendly status values into the contract expected by demand-service
 * ({@code PATCH /api/v1/demands/{id}/status}).
 */
public final class DemandStatusMapper {

    private static final Set<String> DEMAND_SERVICE_STATUSES = Set.of(
            "DRAFT", "PENDING_APPROVAL", "APPROVED", "INTERNAL_SEARCH",
            "OPEN_EXTERNAL", "FILLED", "ON_HOLD", "CLOSED"
    );

    private static final Set<String> OPEN_EXTERNAL_CLOSURE_REASONS = Set.of(
            "NO_INTERNAL_MATCH", "HM_REJECTED_NOMINATION"
    );

    private DemandStatusMapper() {
    }

    public record MappedTransition(String targetStatus, String closureReason) {
    }

    public static MappedTransition toDemandService(String status, String closureReason) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }

        String uiStatus = status.trim().toUpperCase();
        String uiClosureReason = normalizeClosureReason(closureReason);

        return switch (uiStatus) {
            case "FILLED_INTERNAL", "FILLED_EXTERNAL", "FILLED" -> new MappedTransition("FILLED", "FILLED");
            case "CANCELLED" -> new MappedTransition("CLOSED", resolveCancelledClosureReason(uiClosureReason));
            case "DUPLICATE" -> new MappedTransition("CLOSED", "DUPLICATE");
            case "ON_HOLD" -> new MappedTransition("ON_HOLD", resolveOnHoldClosureReason(uiClosureReason));
            case "OPEN_EXTERNAL" -> new MappedTransition("OPEN_EXTERNAL", resolveOpenExternalClosureReason(uiClosureReason));
            case "CLOSED" -> new MappedTransition("CLOSED", requireClosureReason(uiClosureReason, "CLOSED"));
            default -> mapPassthrough(uiStatus, uiClosureReason);
        };
    }

    private static MappedTransition mapPassthrough(String uiStatus, String uiClosureReason) {
        if (!DEMAND_SERVICE_STATUSES.contains(uiStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status '" + uiStatus + "'. Allowed values include demand statuses ("
                            + String.join(", ", DEMAND_SERVICE_STATUSES)
                            + ") and RMG aliases (FILLED_INTERNAL, FILLED_EXTERNAL, CANCELLED, DUPLICATE)"
            );
        }
        return new MappedTransition(uiStatus, uiClosureReason);
    }

    private static String resolveCancelledClosureReason(String uiClosureReason) {
        if (uiClosureReason == null || "CANCELLED".equals(uiClosureReason)) {
            return "WITHDRAWN";
        }
        if ("WITHDRAWN".equals(uiClosureReason)) {
            return uiClosureReason;
        }
        throw new IllegalArgumentException(
                "Invalid closureReason for CANCELLED: '" + uiClosureReason
                        + "'. Use WITHDRAWN or omit closureReason"
        );
    }

    private static String resolveOnHoldClosureReason(String uiClosureReason) {
        if (uiClosureReason == null) {
            throw new IllegalArgumentException("closureReason is required for ON_HOLD. Use ON_HOLD");
        }
        if ("ON_HOLD".equals(uiClosureReason)) {
            return uiClosureReason;
        }
        throw new IllegalArgumentException(
                "Invalid closureReason for ON_HOLD: '" + uiClosureReason + "'. Use ON_HOLD"
        );
    }

    private static String resolveOpenExternalClosureReason(String uiClosureReason) {
        if (uiClosureReason == null) {
            return null;
        }
        if (OPEN_EXTERNAL_CLOSURE_REASONS.contains(uiClosureReason)) {
            return uiClosureReason;
        }
        throw new IllegalArgumentException(
                "Invalid closureReason for OPEN_EXTERNAL: '" + uiClosureReason
                        + "'. Use NO_INTERNAL_MATCH, HM_REJECTED_NOMINATION, or omit closureReason "
                        + "after the internal-search gate has elapsed"
        );
    }

    private static String requireClosureReason(String uiClosureReason, String targetStatus) {
        if (uiClosureReason == null) {
            throw new IllegalArgumentException("closureReason is required for " + targetStatus);
        }
        return uiClosureReason;
    }

    private static String normalizeClosureReason(String closureReason) {
        if (closureReason == null || closureReason.isBlank()) {
            return null;
        }
        String normalized = closureReason.trim().toUpperCase();
        return switch (normalized) {
            case "FILLED_INTERNAL", "FILLED_EXTERNAL" -> "FILLED";
            case "CANCELLED" -> "WITHDRAWN";
            default -> normalized;
        };
    }
}
