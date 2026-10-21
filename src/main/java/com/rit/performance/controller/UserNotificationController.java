package com.rit.performance.controller;

import com.rit.performance.dto.UserNotificationResponse;
import com.rit.performance.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class UserNotificationController {
    private final UserNotificationService notifications;
    @GetMapping
    public Page<UserNotificationResponse> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) String category) {
        return notifications.list(page, size, unreadOnly, category);
    }
    @GetMapping("/unread-count")
    public UnreadCount count() { return new UnreadCount(notifications.unreadCount()); }
    @PatchMapping("/{id}/read")
    public UserNotificationResponse read(@PathVariable Long id) { return notifications.read(id); }
    @PatchMapping("/read-all")
    public MarkedRead readAll() { return new MarkedRead(notifications.readAll()); }
    public record UnreadCount(long count) {}
    public record MarkedRead(int updatedCount) {}
}
