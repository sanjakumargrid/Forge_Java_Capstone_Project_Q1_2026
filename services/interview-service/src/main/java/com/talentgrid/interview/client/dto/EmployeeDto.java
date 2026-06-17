package com.talentgrid.interview.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO projection for an Employee fetched from user-auth-service.
 * Used to resolve interviewer IDs to real corporate email addresses for
 * Google Calendar invites and FreeBusy conflict checks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDto {
    private Long id;
    private String email;
    private String name;
}
