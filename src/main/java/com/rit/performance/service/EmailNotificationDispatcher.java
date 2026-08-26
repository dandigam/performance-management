package com.rit.performance.service;

import com.rit.performance.entity.EmailNotification;
import com.rit.performance.repository.EmailNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class EmailNotificationDispatcher {
    private final EmailNotificationRepository repository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}") private String from;
    @Value("${app.mail.max-retries:3}") private int maxRetries;

    @Scheduled(fixedDelayString = "${app.mail.dispatch-delay-ms:30000}")
    public void dispatch() {
        repository.findReadyToSend(LocalDateTime.now(), maxRetries, PageRequest.of(0, 50))
                .forEach(this::send);
    }

    private void send(EmailNotification notification) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (from != null && !from.isBlank()) message.setFrom(from);
            message.setTo(notification.getRecipientEmail());
            message.setSubject(notification.getSubject());
            message.setText(compose(notification));
            mailSender.send(message);
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
}
