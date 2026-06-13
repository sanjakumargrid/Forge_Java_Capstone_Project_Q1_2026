package com.talentgrid.kafka.events.demand;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DemandPayload {

    // ── Core identifiers ────────────────────────────────────────────────────────
    private Long demandId;//
    private String title;//
    private String status; //

    // ── Seniority & Skills ──────────────────────────────────────────────────────
    private String level; //
    private List<String> skills; //
    private String location;//

    // ── Fill counts ─────────────────────────────────────────────────────────────
    private Integer requiredCount;//
    private Integer internalFilledCount;
    private Integer externalFilledCount;
    private Integer recruitedCount;

    // ── Closure ─────────────────────────────────────────────────────────────────
    private String closureReason;//

    // ── Personnel ───────────────────────────────────────────────────────────────
    private Long createdBy;
    private Long approvedBy;
    private Long assignedRecruiter;
    private Long assignedRm;
    private String recipientEmail;
    private String raisedBy;
    private OffsetDateTime searchStartAt;
}
