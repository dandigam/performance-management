package com.rit.performance.service;

import com.rit.performance.entity.NotificationSubscription.RecipientType;
import com.rit.performance.repository.NotificationSubscriptionRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Resolves optional business notification copies; null category means private email. */
@Service
@RequiredArgsConstructor
public class NotificationRecipientResolver {
    public static final String GLOBAL_CATEGORY = "ALL_NOTIFICATIONS";
    private final NotificationSubscriptionRepository repository;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Recipients resolve(String category, String primary) {
        var to = new LinkedHashSet<String>();
        add(to, primary);
        var cc = new LinkedHashSet<String>();
        var bcc = new LinkedHashSet<String>();
        if (category != null && !category.isBlank()) {
            var codes = new LinkedHashSet<String>();
            codes.add(category.trim().toUpperCase(Locale.ROOT));
            codes.add(GLOBAL_CATEGORY);
            for (var subscription : repository.findActiveForCodes(codes)) {
                if (subscription.getRecipientType() == RecipientType.CC) {
                    add(cc, subscription.getEmailAddresses());
                } else if (subscription.getRecipientType() == RecipientType.BCC) {
                    add(bcc, subscription.getEmailAddresses());
                }
            }
        }
        cc.removeAll(to);
        bcc.removeAll(to);
        // Honor the more private setting when category and global settings overlap.
        cc.removeAll(bcc);
        return new Recipients(List.copyOf(to), List.copyOf(cc), List.copyOf(bcc));
    }

    private static void add(Set<String> target, String emails) {
        if (emails == null || emails.isBlank()) return;
        for (String part : emails.split(",")) {
            if (!part.isBlank()) target.add(part.trim().toLowerCase(Locale.ROOT));
        }
    }

    public static String categoryFor(EmailEventType type) {
        if (type == null) return null;
        return switch (type) {
            case ONBOARDING_SUBMITTED, ONBOARDING_CHANGES_REQUESTED -> "ONBOARDING";
            case CYCLE_PUBLISHED, ASSESSMENT_READY, ASSESSMENT_REOPENED, RESULT_PUBLISHED -> "PERFORMANCE_REVIEW";
            case MANUAL, REMINDER -> GLOBAL_CATEGORY;
            case ONBOARDING_INVITATION, PASSWORD_CHANGED -> null;
        };
    }

    public record Recipients(List<String> to, List<String> cc, List<String> bcc) {
        public boolean isEmpty() { return to.isEmpty() && cc.isEmpty() && bcc.isEmpty(); }
        public void apply(MimeMessageHelper message) throws MessagingException {
            if (!to.isEmpty()) message.setTo(to.toArray(String[]::new));
            if (!cc.isEmpty()) message.setCc(cc.toArray(String[]::new));
            if (!bcc.isEmpty()) message.setBcc(bcc.toArray(String[]::new));
        }
    }
}
