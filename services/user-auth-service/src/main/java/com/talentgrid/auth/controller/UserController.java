package com.talentgrid.auth.controller;

import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * Returns user info including slackId by employee/user ID.
     * Called internally by demand-service via Feign to resolve
     * the creator's Slack ID for notification routing.
     */
    @GetMapping("/{id}")
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
}