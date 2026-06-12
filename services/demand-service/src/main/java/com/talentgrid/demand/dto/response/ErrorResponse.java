package com.talentgrid.demand.dto.response;

import java.time.OffsetDateTime;

/**
 * Standard error response structure for the Demand Service API.
 */
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private OffsetDateTime timestamp;

    public ErrorResponse() {
        this.timestamp = OffsetDateTime.now();
    }

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = OffsetDateTime.now();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
