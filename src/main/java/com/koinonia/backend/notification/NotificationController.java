package com.koinonia.backend.notification;

import com.koinonia.backend.notification.dto.NotificationResponse;
import com.koinonia.backend.notification.dto.UnreadCountResponse;
import com.koinonia.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Page<NotificationResponse> getNotifications(
            Pageable pageable,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return notificationService.findForUser(currentUser, pageable);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        long count = notificationService.unreadCount(currentUser);
        return new UnreadCountResponse(count);
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        long markedRead = notificationService.markAllAsRead(currentUser);
        return ResponseEntity.ok(new java.util.HashMap<String, Long>() {{
            put("markedRead", markedRead);
        }});
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        notificationService.markAsRead(id, currentUser);
        return ResponseEntity.ok(new java.util.HashMap<String, Boolean>() {{
            put("ok", true);
        }});
    }
}
