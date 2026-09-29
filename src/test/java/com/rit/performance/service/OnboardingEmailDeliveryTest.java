package com.rit.performance.service;

import com.rit.performance.entity.EmailNotification;
import com.rit.performance.repository.EmailNotificationRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.List;
import java.util.Properties;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class OnboardingEmailDeliveryTest {
    @Test void attachesLogoToQueuedOnboardingMessage() throws Exception {
        var repository = mock(EmailNotificationRepository.class);
        var sender = mock(JavaMailSender.class);
        var message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        var email = EmailNotification.builder().eventType(EmailEventType.ONBOARDING_INVITATION)
                .recipientEmail("preview@example.com").subject("Welcome to RailInfo Tech!")
                .body("<!DOCTYPE html><html><body><img src=\"cid:rit-logo\"></body></html>").build();
        when(repository.findReadyToSend(any(), anyInt(), any())).thenReturn(List.of(email));
        new EmailNotificationDispatcher(repository, sender, new NotificationRecipientResolver(mock(com.rit.performance.repository.NotificationSubscriptionRepository.class))).dispatch();
        verify(sender).send(message);
        message.saveChanges();
        var output = new java.io.ByteArrayOutputStream();
        message.writeTo(output);
        String mime = output.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(mime.contains("Content-ID: <rit-logo>"));
        assertTrue(mime.contains("image/png"));
        assertEquals(EmailDeliveryStatus.SENT, email.getStatus());
    }
}
