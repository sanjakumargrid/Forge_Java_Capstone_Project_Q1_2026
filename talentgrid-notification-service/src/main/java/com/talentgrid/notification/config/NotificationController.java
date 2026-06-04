package com.talentgrid.notification.config;

import com.talentgrid.notification.model.Notification;
import com.talentgrid.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for in-app notification management.
 *
 * <p>In production this endpoint is secured via the auth-service JWT filter.
 * For dev/demo, auth is disabled in {@code application.properties}.</p>
 *
 * <p>Base path: {@code /api/notifications}</p>
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Returns all notifications for a given user (most recent first).
     * GET /api/notifications?userId=101
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(
            @RequestParam Long userId) {
        return ResponseEntity.ok(notificationService.getNotificationsForUser(userId));
    }

    /**
     * Returns only unread notifications.
     * GET /api/notifications/unread?userId=101
     */
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnread(
            @RequestParam Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotificationsForUser(userId));
    }

    /**
     * Returns unread notification count (for the bell badge).
     * GET /api/notifications/count?userId=101
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestParam Long userId) {
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount(userId)));
    }

    /**
     * Marks a single notification as read.
     * PATCH /api/notifications/{id}/read?userId=101
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @RequestParam Long userId) {
        boolean updated = notificationService.markAsRead(id, userId);
        return updated ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Marks all notifications for a user as read.
     * PATCH /api/notifications/read-all?userId=101
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
            @RequestParam Long userId) {
        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("markedRead", count));
    }
}
