package com.talentgrid.auth.controller;

import com.talentgrid.auth.service.interfaces.UserSecurityCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final UserSecurityCacheService userSecurityCacheService;

    @Autowired
    public TestController(UserSecurityCacheService userSecurityCacheService) {
        this.userSecurityCacheService = userSecurityCacheService;
    }

    @GetMapping("/cache/{userId}")
    public Object cache(
            @PathVariable Long userId
    ) {
        return userSecurityCacheService.getUser(userId);
    }

    /**
     * GENERAL AUTH TEST
     */
    @GetMapping("/secure")
    public ResponseEntity<Map<String, Object>> secureApi(
            Authentication authentication
    ) {

        Map<String, Object> response = new HashMap<>();

        response.put(
                "message",
                "JWT authentication successful"
        );

        response.put(
                "loggedInUser",
                authentication.getName()
        );

        response.put(
                "authorities",
                authentication.getAuthorities()
        );

        response.put(
                "timestamp",
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * ROLE + SCOPE TEST
     */
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> adminApi(
            Authentication authentication
    ) {

        Map<String, Object> response = new HashMap<>();

        response.put(
                "message",
                "RBAC WORKING - ADMIN ACCESS GRANTED"
        );

        response.put(
                "loggedInUser",
                authentication.getName()
        );

        response.put(
                "authorities",
                authentication.getAuthorities()
        );

        response.put(
                "timestamp",
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }
}