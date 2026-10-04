package com.rit.performance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationEmailSender {
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final NotificationRecipientResolver recipientResolver;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${app.mail.from:}")
    private String from;

    @TransactionalEventListener
    public void send(ApplicationEmail event) {
        if (!enabled) {
            return;
        }

        try {
            var recipients = recipientResolver.resolve(event.category(), event.recipient());
            if (recipients.isEmpty()) return;
            JavaMailSender mailSender = mailSenders.getObject();
            MimeMessageHelper message = new MimeMessageHelper(mailSender.createMimeMessage(), true,
                    java.nio.charset.StandardCharsets.UTF_8.name());
            if (from != null && !from.isBlank()) {
                message.setFrom(from);
            }
            recipients.apply(message);
            message.setSubject(event.subject());
            EmailBranding.setContent(message, event.body(), event.html());
            mailSender.send(message.getMimeMessage());
        } catch (Exception exception) {
            // Do not log message content because it may contain a password reset token.
            log.warn("Transactional email delivery failed ({})", exception.getClass().getSimpleName());
        }
    }
}
