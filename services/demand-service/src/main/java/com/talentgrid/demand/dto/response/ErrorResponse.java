package com.talentgrid.demand.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Standard error response structure for the Demand Service API.
 */
@Data
@NoArgsConstructor
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private OffsetDateTime timestamp = OffsetDateTime.now();

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = OffsetDateTime.now();
    }
}
