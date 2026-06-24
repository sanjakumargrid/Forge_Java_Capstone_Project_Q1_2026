package com.talentgrid.jobposting.controller;

import com.talentgrid.jobposting.dto.response.NotificationResponse;
import com.talentgrid.jobposting.security.AuthenticatedUser;
import com.talentgrid.jobposting.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * In-app notifications for the authenticated user (identified from the JWT,
 * not a path/query parameter).
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** Lists all notifications for the current user. */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAll(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(notificationService.getForUser(user.getUserId()));
    }

    /** Lists unread notifications for the current user. */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnread(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(notificationService.getUnreadForUser(user.getUserId()));
    }

    /** Gets unread notification count for the current user (notification bell badge). */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> countUnread(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(notificationService.countUnread(user.getUserId()));
    }

    /** Marks all notifications as read for the current user. */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal AuthenticatedUser user) {
        notificationService.markAllRead(user.getUserId());
        return ResponseEntity.noContent().build();
    }

    /** Marks a single notification as read. */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markOneRead(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        notificationService.markOneRead(id, user.getUserId());
        return ResponseEntity.noContent().build();
    }
}
