package com.rit.performance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetEmailSender {
    private final org.springframework.beans.factory.ObjectProvider<JavaMailSender> mailSenders;
    @Value("${app.mail.enabled:false}") private boolean enabled;
    @Value("${app.mail.from:}") private String from;

    @TransactionalEventListener
    public void send(PasswordResetEmail event) {
        if (!enabled) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (from != null && !from.isBlank()) message.setFrom(from);
            message.setTo(event.email());
            message.setSubject("Reset your RIT Performance Management password");
            message.setText("A password reset was requested for your account.\n\n"
                    + "Use this link to choose a new password:\n" + event.link()
                    + "\n\nThis link expires in 30 minutes and can be used only once."
                    + " If you did not request this, you can ignore this email.");
            mailSenders.getObject().send(message);
        } catch (Exception exception) {
            // SMTP exception details can include the message body or reset link.
            log.warn("Password reset email delivery failed; the user can request another link.");
        }
    }
}
