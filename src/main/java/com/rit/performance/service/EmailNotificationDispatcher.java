package com.rit.performance.service;

import com.rit.performance.entity.EmailNotification;
import com.rit.performance.repository.EmailNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class EmailNotificationDispatcher {
    private final EmailNotificationRepository repository;
    private final JavaMailSender mailSender;
    private final NotificationRecipientResolver recipientResolver;

    @Value("${app.mail.from:}") private String from;
    @Value("${app.mail.max-retries:3}") private int maxRetries;

    @Scheduled(fixedDelayString = "${app.mail.dispatch-delay-ms:30000}")
    public void dispatch() {
        repository.findReadyToSend(LocalDateTime.now(), maxRetries, PageRequest.of(0, 50))
                .forEach(this::send);
    }

    private void send(EmailNotification notification) {
        try {
            MimeMessageHelper message = new MimeMessageHelper(mailSender.createMimeMessage(), true,
                    java.nio.charset.StandardCharsets.UTF_8.name());
            if (from != null && !from.isBlank()) message.setFrom(from);
            var recipients = recipientResolver.resolve(
                    NotificationRecipientResolver.categoryFor(notification.getEventType()),
                    notification.getRecipientEmail());
            if (recipients.isEmpty()) {
                notification.setStatus(EmailDeliveryStatus.SKIPPED);
                notification.setNextAttemptDate(null);
                notification.setErrorMessage(null);
                repository.save(notification);
                return;
            }
            recipients.apply(message);
            message.setSubject(notification.getSubject());
            String content = compose(notification);
            EmailBranding.setContent(message, content, isHtml(content));
            mailSender.send(message.getMimeMessage());
            notification.setStatus(EmailDeliveryStatus.SENT);
            notification.setSentDate(LocalDateTime.now());
            notification.setErrorMessage(null);
            notification.setNextAttemptDate(null);
        } catch (Exception exception) {
            int attempts = notification.getRetryCount() + 1;
            notification.setRetryCount(attempts);
            notification.setErrorMessage(truncate(exception.getMessage(), 2000));
            if (attempts >= maxRetries) {
                notification.setStatus(EmailDeliveryStatus.FAILED);
                notification.setNextAttemptDate(null);
            } else {
                notification.setStatus(EmailDeliveryStatus.PENDING);
                notification.setNextAttemptDate(LocalDateTime.now().plusMinutes((long) attempts * attempts));
            }
        }
        repository.save(notification);
    }

    private String compose(EmailNotification notification) {
        if (isHtml(notification.getBody())) {
            StringBuilder extras = new StringBuilder();
            if (notification.getActionUrl() != null && !notification.getActionUrl().isBlank())
                extras.append("<p>Open: ").append(org.springframework.web.util.HtmlUtils.htmlEscape(notification.getActionUrl())).append("</p>");
            if (notification.getFooter() != null && !notification.getFooter().isBlank())
                extras.append("<p>").append(org.springframework.web.util.HtmlUtils.htmlEscape(notification.getFooter())).append("</p>");
            String body = notification.getBody();
            int end = body.toLowerCase(java.util.Locale.ROOT).lastIndexOf("</body>");
            return end < 0 ? body + extras : body.substring(0, end) + extras + body.substring(end);
        }
        StringBuilder text = new StringBuilder(notification.getBody());
        if (notification.getActionUrl() != null && !notification.getActionUrl().isBlank())
            text.append("\n\nOpen: ").append(notification.getActionUrl());
        if (notification.getFooter() != null && !notification.getFooter().isBlank())
            text.append("\n\n").append(notification.getFooter());
        return text.toString();
    }

    private String truncate(String value, int max) {
        if (value == null) return "Unknown email delivery error";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private boolean isHtml(String content) {
        String value = content == null ? "" : content.stripLeading().toLowerCase(java.util.Locale.ROOT);
        return value.startsWith("<!doctype html") || value.startsWith("<html");
    }
}
