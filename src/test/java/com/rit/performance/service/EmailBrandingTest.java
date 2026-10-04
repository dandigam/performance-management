package com.rit.performance.service;

import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailBrandingTest {
    @Test
    void everyTemplateRendersWithOneLogoAndCorrectPaymentLabels() throws Exception {
        var factory = new ApplicationEmailFactory(mock(UserRepository.class));
        ReflectionTestUtils.setField(factory, "frontendUrl", "https://portal.example.com");
        ReflectionTestUtils.setField(factory, "footer", "Regards, RailInfo Tech");
        var employee = new Employee();
        employee.setFirstName("Jane");
        employee.setLastName("Doe");
        employee.setEmail("jane@example.com");
        var user = new User();
        user.setEmployee(employee);
        user.setUsername(employee.getEmail());
        var cycle = new PerformanceCycles();
        cycle.setCycleName("Annual review");
        var review = new EmployeeReview();
        review.setEmployee(employee);
        review.setPerformanceCycle(cycle);
        var assessment = new EmployeeReviewAssessment();
        assessment.setAssessorEmployee(employee);
        var rating = new FinalRating();
        rating.setEmployeeReview(review);
        var sow = new Sow();
        sow.setId(1L);
        sow.setSowName("Sample Test");
        var invoice = new SowInvoice();
        invoice.setSow(sow);
        invoice.setMilestone(new SowMilestone());
        var payment = new SowInvoicePayment();
        payment.setInvoice(invoice);
        payment.setPaymentReference("PAY-123");
        var sheet = new Timesheet();
        sheet.setEmployee(employee);
        sheet.setStatus(TimesheetStatus.LEVEL1_APPROVED);
        var emails = List.of(
                factory.employeeCreated(employee, user), factory.employeeAdminNotification(employee, true),
                factory.manualNotification(employee.getEmail(), "Jane", "Update", "Details", null),
                factory.onboardingInvitation(employee, "Engineer", "https://example.com/setup"),
                factory.userInvitation(employee.getEmail(), "Jane", "https://example.com/setup"),
                factory.passwordReset(employee.getEmail(), "https://example.com/reset"),
                factory.passwordChanged(user), factory.cyclePublished(cycle, employee),
                factory.assessmentReady(review, assessment),
                factory.assessmentReopened(review, assessment, java.time.LocalDate.of(2026, 10, 10), "Correction"),
                factory.resultPublished(rating), factory.sowCreated(sow), factory.invoice(invoice, "created"),
                factory.invoicePayment(payment, "created"),
                factory.timesheetWorkflow(sheet, "approved by Level 1", null).get(0));
        for (var email : emails) {
            assertEquals(1, email.body().split("cid:rit-logo", -1).length - 1, email.subject());
            assertFalse(java.util.regex.Pattern.compile("\\s+th:[\\w-]+=").matcher(email.body()).find(), email.subject());
        }
        String paymentBody = factory.invoicePayment(payment, "created").body();
        assertTrue(paymentBody.contains("Payment date"));
        assertTrue(paymentBody.contains("Received amount"));
        assertTrue(paymentBody.contains("Payment reference"));
        assertFalse(paymentBody.contains("Status / reference"));
        assertTrue(emails.get(emails.size() - 1).body().contains("Level 1 approved"));
        var preview = java.nio.file.Path.of("target/email-preview/sow.html");
        java.nio.file.Files.createDirectories(preview.getParent());
        java.nio.file.Files.writeString(preview, factory.sowCreated(sow).body()
                .replace("cid:rit-logo", "../../src/main/resources/email/rit-logo.png"));
    }

    @ParameterizedTest
    @EnumSource(EmailEventType.class)
    void embedsLogoForEveryQueuedEvent(EmailEventType type) throws Exception {
        var repository = mock(EmailNotificationRepository.class);
        var sender = mock(JavaMailSender.class);
        var message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        var notification = EmailNotification.builder().eventType(type)
                .recipientEmail("test@example.com").subject("Notification")
                .body("<!doctype html><html><body><p>Existing notification</p></body></html>")
                .footer("Regards <team>").build();
        when(repository.findReadyToSend(any(), anyInt(), any())).thenReturn(List.of(notification));
        new EmailNotificationDispatcher(repository, sender, resolver()).dispatch();
        verify(sender).send(message);
        assertEquals(EmailDeliveryStatus.SENT, notification.getStatus());
        assertLogo(message);
        String html = html(message);
        assertTrue(html.contains("Existing notification"));
        assertTrue(html.contains("Regards &lt;team&gt;"));
        assertTrue(html.indexOf("Regards") < html.indexOf("</body>"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void embedsLogoForTransactionalHtmlAndEscapesPlainText() throws Exception {
        var sender = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(sender);
        var delivery = new ApplicationEmailSender(provider, resolver());
        ReflectionTestUtils.setField(delivery, "enabled", true);
        for (boolean html : List.of(true, false)) {
            var message = new MimeMessage(Session.getInstance(new Properties()));
            when(sender.createMimeMessage()).thenReturn(message);
            delivery.send(new ApplicationEmail("test@example.com", "Test",
                    html ? "<html><body><img src=\"cid:rit-logo\">Hello</body></html>" : "Hello <team> & colleagues", html));
            verify(sender).send(message);
            assertLogo(message);
            if (!html) assertTrue(html(message).contains("Hello &lt;team&gt; &amp; colleagues"));
        }
    }

    @Test
    void sowStatusUsesActualNameAndNewStatusWithSafeFallbacks() {
        var users = mock(UserRepository.class);
        var factory = new ApplicationEmailFactory(users);
        ReflectionTestUtils.setField(factory, "frontendUrl", "https://portal.example.com");
        var employee = new Employee();
        employee.setFirstName("Jane");
        employee.setLastName("Doe");
        var user = new User();
        user.setEmployee(employee);
        user.setUsername("jane.doe");
        when(users.findCurrentUser(1L)).thenReturn(Optional.of(user));
        var sow = new Sow();
        sow.setId(21L);
        sow.setSowName("Sample Test");
        sow.setUpdatedBy(1L);
        var status = new LookupValue();
        status.setName("Waiting for Approval");
        sow.setStatus(status);
        var email = factory.sowUpdated(sow, "status updated");
        assertTrue(email.body().contains("The status of Sample Test was changed to Waiting for Approval by Jane Doe."));
        assertTrue(email.body().contains("cid:rit-logo"));
        assertFalse(email.body().contains("User 1"));
        user.setEmployee(null);
        assertTrue(factory.sowUpdated(sow, "status updated").body().contains("by jane.doe."));
        user.setUsername("<script>name</script>");
        assertFalse(factory.sowUpdated(sow, "status updated").body().contains("<script>"));
        sow.setUpdatedBy(null);
        assertTrue(factory.sowUpdated(sow, "status updated").body().contains("by System."));
        sow.setUpdatedBy(99L);
        assertTrue(factory.sowUpdated(sow, "signature updated").body()
                .contains("The signature for Sample Test was updated by Unknown user."));
    }

    private NotificationRecipientResolver resolver() {
        return new NotificationRecipientResolver(mock(NotificationSubscriptionRepository.class));
    }

    private void assertLogo(MimeMessage message) throws Exception {
        message.saveChanges();
        assertEquals(1, logoCount(message));
        assertTrue(html(message).contains("cid:rit-logo"));
    }

    private int logoCount(Part part) throws Exception {
        var ids = part.getHeader("Content-ID");
        if (ids != null && List.of(ids).contains("<rit-logo>")) {
            assertTrue(part.isMimeType("image/png"));
            assertArrayEquals(new org.springframework.core.io.ClassPathResource("email/rit-logo.png")
                    .getContentAsByteArray(), part.getInputStream().readAllBytes());
            return 1;
        }
        int count = 0;
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) count += logoCount(multipart.getBodyPart(i));
        }
        return count;
    }

    private String html(Part part) throws Exception {
        if (part.isMimeType("text/html")) return (String) part.getContent();
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String html = html(multipart.getBodyPart(i));
                if (!html.isEmpty()) return html;
            }
        }
        return "";
    }
}
