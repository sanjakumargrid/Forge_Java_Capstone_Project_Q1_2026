package com.talentgrid.auth.service.interfaces;

import com.talentgrid.auth.dto.response.UserSummaryResponse;

/**
 * Service for lightweight user lookup operations used by
 * other services (Demand Service, Workforce Service, etc.).
 */
public interface UserLookupService {

  /**
   * Returns the resource manager responsible for the given location.
   *
   * Example:
   * Chennai -> Chennai resource manager
   * Bangalore -> Bangalore resource manager
   *
   * @param location employee/project/demand location
   * @return resource manager user summary
   */
  UserSummaryResponse getRmgByLocation(String location);
}