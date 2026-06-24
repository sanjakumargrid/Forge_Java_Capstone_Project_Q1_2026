package com.talentgrid.demand.dto.request;

import com.talentgrid.demand.domain.enums.ClosureReason;
import com.talentgrid.demand.domain.enums.DemandStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for approving or rejecting a pending demand.
 *
 * <p>Valid {@link #decision} values from {@code PENDING_APPROVAL}:
 * {@code APPROVED}, {@code CLOSED} (reject — requires {@link #closureReason} {@code PM_REJECTED}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {

    private DemandStatus decision;

    /** Required when {@code decision} is {@code CLOSED} (typically {@code PM_REJECTED}). */
    private ClosureReason closureReason;

    private String comments;

    private Long assignedRecruiter;
    private String assignedRecruiterName;

    private Long assignedRm;
    private String assignedRmName;
}
