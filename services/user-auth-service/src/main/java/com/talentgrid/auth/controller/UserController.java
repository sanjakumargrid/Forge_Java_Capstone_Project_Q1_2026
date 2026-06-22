package com.talentgrid.auth.controller;

import com.talentgrid.auth.dto.response.UserSummaryResponse;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.security.CachedUserPrincipal;
import com.talentgrid.auth.service.interfaces.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
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
    @GetMapping("/users/rmg-by-location")
    public ResponseEntity<UserSummaryResponse> getRmgByLocation(
            @RequestParam String location) {

        return ResponseEntity.ok(
                userLookupService.getRmgByLocation(location)
        );
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<Map<String, Object>> getAccountById(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "id", id,
                "name", "Mock Account",
                "accountManagerId", 1L
        ));
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<Map<String, Object>> getProjectById(@PathVariable Long id) {
        Long pmId = userRepository.findByEmail("projectmanager@griddynamics.com")
                .map(User::getId)
                .orElse(2L); // default fallback
        return ResponseEntity.ok(Map.of(
                "id", id,
                "accountId", 1L,
                "name", "Mock Project",
                "projectManagerId", pmId
        ));
    }

    @GetMapping("/projects/mine-as-pm")
    public ResponseEntity<List<Map<String, Object>>> getMyProjectsAsPm(Authentication authentication) {
        Long pmId = null;
        if (authentication != null && authentication.getPrincipal() instanceof CachedUserPrincipal) {
            pmId = ((CachedUserPrincipal) authentication.getPrincipal()).getUserId();
        }
        if (pmId == null) {
            pmId = userRepository.findByEmail("projectmanager@griddynamics.com")
                    .map(User::getId)
                    .orElse(2L);
        }
        return ResponseEntity.ok(List.of(Map.of(
                "id", 1L,
                "name", "Mock Project",
                "accountId", 1L,
                "projectManagerId", pmId
        )));
    }
}