package com.rit.performance.service;

import com.rit.performance.entity.UserNotification;
import com.rit.performance.repository.*;
import com.rit.performance.security.AppUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.*;

/** Explicit business metadata only: never turns arbitrary email bodies into inbox content. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class BusinessEmailNotificationListener {
    private final UserRepository users;
    private final UserNotificationRepository notifications;
    private final NotificationSubscriptionRepository subscriptions;
    private final AppUserDetailsService userDetails;

    @EventListener
    public void onQueuedEmail(NotificationEvents.BusinessEmailAlert event) {
        onEmail(event.email());
    }

    @EventListener
    public void onEmail(ApplicationEmail email) {
        var alert = email.bellAlert();
        if (alert == null || alert.recordId() == null) return;
        Set<String> addresses = new HashSet<>();
        addAddresses(addresses, email.recipient());
        if (email.category() != null) {
            Set<String> categories = new HashSet<>();
            categories.add(email.category());
            categories.add(NotificationRecipientResolver.GLOBAL_CATEGORY);
            subscriptions.findActiveForCodes(categories)
                    .forEach(subscription -> addAddresses(addresses, subscription.getEmailAddresses()));
        }
        String key = "BUSINESS:" + alert.occurrence();
        for (var candidate : users.findAllByOrderByIdAsc()) {
            var employee = candidate.getEmployee();
            boolean recipient = matches(addresses, candidate.getUsername())
                    || (employee != null && (matches(addresses, employee.getEmail())
                        || alert.employeeIds().contains(employee.getId())))
                    || (alert.includeAdmins() && "ROLE_ADMIN".equals(userDetails.roleAuthority(candidate.getRole())));
            if (!recipient || !"ACTIVE".equalsIgnoreCase(candidate.getStatus())
                    || !"FULL".equalsIgnoreCase(candidate.getPortalAccess())) continue;
            var user = users.findForSecurityUpdate(candidate.getId()).orElse(null);
            if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())
                    || !"FULL".equalsIgnoreCase(user.getPortalAccess())
                    || notifications.findForEvent(user.getId(), key).isPresent()) continue;
            var notification = new UserNotification();
            notification.setRecipient(user);
            notification.setCategory(alert.category());
            notification.setEventType(alert.eventType());
            notification.setTitle(truncate(email.subject(), 200));
            notification.setMessage(truncate(email.subject(), 900) + ". Open the related record for details.");
            notification.setRelatedRecordType(alert.recordType());
            notification.setRelatedRecordId(alert.recordId());
            notification.setDeduplicationKey(key);
            notification.setCreatedOn(Instant.now());
            notifications.save(notification);
        }
    }

    private static boolean matches(Set<String> addresses, String value) {
        return value != null && addresses.contains(value.trim().toLowerCase(Locale.ROOT));
    }
    private static void addAddresses(Set<String> addresses, String value) {
        if (value == null) return;
        for (String address : value.split(",")) {
            if (!address.isBlank()) addresses.add(address.trim().toLowerCase(Locale.ROOT));
        }
    }
    private static String truncate(String value, int max) {
        if (value == null) return "Business notification";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
