package com.talentgrid.demand.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO projection representing a User from the User Auth Service.
 * Used to fetch user-specific details such as the Slack ID during demand creation,
 * ensuring notification recipients are resolved dynamically rather than hard-coded.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String slackId;
}