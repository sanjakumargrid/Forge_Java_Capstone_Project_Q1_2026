package com.talentgrid.auth.controller;

import com.talentgrid.auth.dto.response.UserSummaryResponse;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.service.interfaces.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserLookupService userLookupService;

    /**
     * Returns user info including slackId by employee/user ID.
     * Called internally by demand-service via Feign to resolve
     * the creator's Slack ID for notification routing.
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(Map.of(
                        "id",      user.getId(),
                        "name",    user.getUsername(),
                        "email",   user.getEmail(),
                        "slackId", user.getSlackId() != null ? user.getSlackId() : ""
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns the active RMG assigned to the given location.
     *
     * Example:
     * GET /api/v1/users/rmg-by-location?location=Chennai
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
    @GetMapping("/users/rmg-by-location")
    public ResponseEntity<UserSummaryResponse> getRmgByLocation(
            @RequestParam String location) {

        return ResponseEntity.ok(
                userLookupService.getRmgByLocation(location)
        );
    }

}