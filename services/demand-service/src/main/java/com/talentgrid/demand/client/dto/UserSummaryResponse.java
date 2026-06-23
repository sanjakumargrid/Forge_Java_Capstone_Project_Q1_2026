package com.talentgrid.demand.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight user projection returned by user-auth-service.
 *
 * Used by:
 * - Approval SLA reminders
 * - RMG lookup by location
 * - Future notification routing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {

  /**
   * Employee/User ID.
   */
  private Long id;

  /**
   * Full name.
   */
  private String username;

  /**
   * Email address.
   */
  private String email;

  /**
   * User location.
   */
  private String location;

  /**
   * User slack ID.
   */
  private String slackId;
}