package com.talentgrid.auth.service.interfaces;

import com.talentgrid.auth.dto.response.UserSummaryResponse;

/**
 * Service for lightweight user lookup operations used by
 * other services (Demand Service, Workforce Service, etc.).
 */
public interface UserLookupService {

  /**
   * Returns the RMG responsible for the given location.
   *
   * Example:
   * Chennai -> Chennai RMG
   * Bangalore -> Bangalore RMG
   *
   * @param location employee/project/demand location
   * @return RMG user summary
   */
  UserSummaryResponse getRmgByLocation(String location);
}