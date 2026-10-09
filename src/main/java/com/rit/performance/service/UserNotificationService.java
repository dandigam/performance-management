package com.rit.performance.service;

import com.rit.performance.dto.UserNotificationResponse;
import com.rit.performance.entity.UserNotification;
import com.rit.performance.exception.*;
import com.rit.performance.repository.*;
import com.rit.performance.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class UserNotificationService {
    private final UserNotificationRepository notifications;
    private final UserRepository users;
    private static final Set<String> CATEGORIES = Set.of("LEAVE", "TIMESHEET", "ONBOARDING", "PERFORMANCE_REVIEW",
            "SOW", "EMPLOYEE", "RESOURCE_ALLOCATION", "INVOICE", "GENERAL");

    public Page<UserNotificationResponse> list(int page, int size, boolean unreadOnly, String category) {
        Long userId = currentUserId();
        if (page < 0 || size < 1 || size > 100) throw new InvalidOperationException("Use page >= 0 and size from 1 to 100");
        String filter = category == null || category.isBlank() ? null : category.trim().toUpperCase(Locale.ROOT);
        if (filter != null && !CATEGORIES.contains(filter)) throw new InvalidOperationException("Unknown notification category");
        return notifications.inbox(userId, unreadOnly, filter,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdOn", "id"))).map(this::response);
    }

    public long unreadCount() { return notifications.countByRecipientIdAndReadAtIsNull(currentUserId()); }

    @Transactional
    public UserNotificationResponse read(Long id) {
        Long userId = currentUserId();
        notifications.markRead(id, userId, Instant.now());
        return response(notifications.findByIdAndRecipientId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found")));
    }

    @Transactional
    public int readAll() { return notifications.markAllRead(currentUserId(), Instant.now()); }

    private Long currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser principal))
            throw new AuthenticationException("Authentication is required");
        return users.findById(principal.id()).filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus()))
                .orElseThrow(() -> new AuthenticationException("Active account is required")).getId();
    }

    private UserNotificationResponse response(UserNotification n) {
        return new UserNotificationResponse(n.getId(), n.getCategory(), n.getEventType(), n.getTitle(),
                n.getMessage(), n.getCreatedOn(), n.getReadAt() != null, n.getReadAt(),
                n.getRelatedRecordType(), n.getRelatedRecordId());
    }
}
