package com.talentgrid.auth.controller;

import com.talentgrid.auth.dto.response.UserSummaryResponse;
import com.talentgrid.auth.service.interfaces.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * User lookup APIs used by other TalentGrid services.
 *
 * Current use case:
 * - Demand Service Approval SLA reminders
 * - RMG lookup by location
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserLookupController {

  private final UserLookupService userLookupService;

  /**
   * Returns the active RMG assigned to the given location.
   *
   * Example:
   * GET /api/users/rmg-by-location?location=Chennai
   *
   * Response:
   * {
   *   "id": 1,
   *   "name": "Chennai RMG",
   *   "email": "rmg.chennai@company.com",
   *   "location": "Chennai"
   * }
   *
   * @param location demand/project location
   * @return RMG user summary
   */
  @GetMapping("/rmg-by-location")
  public ResponseEntity<UserSummaryResponse> getRmgByLocation(
          @RequestParam String location) {

    return ResponseEntity.ok(
            userLookupService.getRmgByLocation(location)
    );
  }
}