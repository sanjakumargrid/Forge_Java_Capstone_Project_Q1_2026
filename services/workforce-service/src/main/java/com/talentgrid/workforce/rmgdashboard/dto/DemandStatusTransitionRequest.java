package com.talentgrid.workforce.rmgdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outbound Feign payload for demand-service {@code PATCH /api/v1/demands/{id}/status}.
 * Field values must match demand-service {@code DemandStatus} and {@code ClosureReason} enum names.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DemandStatusTransitionRequest {
    private String targetStatus;
    private String closureReason;
    private String comments;
}
