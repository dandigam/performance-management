package com.rit.performance.service;

import com.rit.performance.entity.NotificationSubscription;
import com.rit.performance.entity.NotificationSubscription.RecipientType;
import com.rit.performance.repository.NotificationSubscriptionRepository;
import jakarta.mail.Session;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class NotificationRecipientResolverTest {
    private final NotificationSubscriptionRepository repository = mock(NotificationSubscriptionRepository.class);
    private final NotificationRecipientResolver resolver = new NotificationRecipientResolver(repository);

    private NotificationSubscription subscription(String emails, RecipientType type) {
        var result = new NotificationSubscription();
        result.setEmailAddresses(emails);
        result.setRecipientType(type);
        return result;
    }

    @Test void mergesCategoryAndGlobalWithToPrecedenceAndBccPrivacy() {
        when(repository.findActiveForCodes(any())).thenReturn(List.of(
                subscription("hr@example.com,Employee@example.com,admin@example.com", RecipientType.CC),
                subscription("ADMIN@example.com,finance@example.com", RecipientType.BCC)));
        var recipients = resolver.resolve("onboarding", "employee@example.com");
        assertThat(recipients.to()).containsExactly("employee@example.com");
        assertThat(recipients.cc()).containsExactly("hr@example.com");
        assertThat(recipients.bcc()).containsExactly("admin@example.com", "finance@example.com");
        verify(repository).findActiveForCodes(Set.of("ONBOARDING", "ALL_NOTIFICATIONS"));
    }

    @Test void missingGroupsPreservePrimaryAndEmptyRecipientsAreSkipped() {
        assertThat(resolver.resolve("MISSING", "lead@example.com").to()).containsExactly("lead@example.com");
        assertThat(resolver.resolve("MISSING", null).isEmpty()).isTrue();
        when(repository.findActiveForCodes(any())).thenReturn(List.of(subscription("admin@example.com", RecipientType.BCC)));
        assertThat(resolver.resolve("MISSING", null).bcc()).containsExactly("admin@example.com");
    }

    @Test void privateEmailsNeverReadSubscriptions() {
        assertThat(resolver.resolve(null, "employee@example.com").cc()).isEmpty();
        verifyNoInteractions(repository);
        assertThat(NotificationRecipientResolver.categoryFor(EmailEventType.ONBOARDING_INVITATION)).isNull();
        assertThat(NotificationRecipientResolver.categoryFor(EmailEventType.PASSWORD_CHANGED)).isNull();
        assertThat(NotificationRecipientResolver.categoryFor(EmailEventType.ONBOARDING_SUBMITTED)).isEqualTo("ONBOARDING");
    }

    @Test void queuedCategoryOnlyNotificationSendsCopiesOrSkipsWhenMissing() throws Exception {
        var queue = mock(com.rit.performance.repository.EmailNotificationRepository.class);
        var mail = mock(JavaMailSender.class);
        var message = new MimeMessage(Session.getInstance(new Properties()));
        when(mail.createMimeMessage()).thenReturn(message);
        var notification = com.rit.performance.entity.EmailNotification.builder()
                .eventType(EmailEventType.ONBOARDING_SUBMITTED).recipientEmail("")
                .subject("Submitted").body("Review onboarding").build();
        when(queue.findReadyToSend(any(), anyInt(), any())).thenReturn(List.of(notification));
        var dispatcher = new EmailNotificationDispatcher(queue, mail, resolver);
        dispatcher.dispatch();
        assertThat(notification.getStatus()).isEqualTo(EmailDeliveryStatus.SKIPPED);
        verify(mail, never()).send(any(MimeMessage.class));
        when(repository.findActiveForCodes(any())).thenReturn(List.of(
                subscription("admin@example.com", RecipientType.BCC)));
        dispatcher.dispatch();
        assertThat(notification.getStatus()).isEqualTo(EmailDeliveryStatus.SENT);
        verify(mail).send(message);
        assertThat(message.getRecipients(Message.RecipientType.TO)).isNull();
        assertThat(message.getRecipients(Message.RecipientType.BCC)[0].toString()).isEqualTo("admin@example.com");
    }

    @Test void senderAppliesCopiesAndSkipsUnaddressedMessages() throws Exception {
        var mail = mock(JavaMailSender.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(mail);
        var message = new MimeMessage(Session.getInstance(new Properties()));
        when(mail.createMimeMessage()).thenReturn(message);
        var sender = new ApplicationEmailSender(provider, resolver);
        ReflectionTestUtils.setField(sender, "enabled", true);
        sender.send(new ApplicationEmail(null, "No recipients", "body", false, "SOW"));
        verifyNoInteractions(mail);
        when(repository.findActiveForCodes(any())).thenReturn(List.of(
                subscription("hr@example.com", RecipientType.CC),
                subscription("admin@example.com", RecipientType.BCC)));
        sender.send(new ApplicationEmail("employee@example.com", "Subject", "Body", false, "LEAVE"));
        verify(mail).send(message);
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("employee@example.com");
        assertThat(message.getRecipients(Message.RecipientType.CC)[0].toString()).isEqualTo("hr@example.com");
        assertThat(message.getRecipients(Message.RecipientType.BCC)[0].toString()).isEqualTo("admin@example.com");
    }
}
