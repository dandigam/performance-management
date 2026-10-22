package com.rit.performance.service;

import com.rit.performance.entity.Sow;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.SowRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class SowPdfEmailService {
    private static final int MAX_ATTACHMENT_SIZE_BYTES = 10 * 1024 * 1024;
    private static final byte[] PDF_HEADER = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final SowRepository sowRepository;
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final ThreadPoolTaskExecutor emailExecutor;
    private final boolean emailEnabled;
    private final String fromAddress;

    public SowPdfEmailService(
            SowRepository sowRepository,
            ObjectProvider<JavaMailSender> mailSenders,
            @Qualifier("sowPdfEmailTaskExecutor") ThreadPoolTaskExecutor emailExecutor,
            @Value("${app.mail.enabled:false}") boolean emailEnabled,
            @Value("${app.mail.from:}") String fromAddress) {
        this.sowRepository = sowRepository;
        this.mailSenders = mailSenders;
        this.emailExecutor = emailExecutor;
        this.emailEnabled = emailEnabled;
        this.fromAddress = fromAddress;
    }

    public void send(Long sowId, Long submittedSowId, String toEmail, MultipartFile attachment) {
        if (sowId == null || !sowId.equals(submittedSowId)) {
            throw new InvalidOperationException("Submitted SOW id must match the URL SOW id");
        }
        String recipient = validateEmail(toEmail);
        byte[] pdf = readPdf(attachment);
        Sow sow = sowRepository.findById(sowId)
                .orElseThrow(() -> new ResourceNotFoundException("SOW not found: " + sowId));
        if (!emailEnabled) {
            throw new InvalidOperationException("EMAIL_DISABLED", "SOW email delivery is disabled");
        }

        String subject = "SOW document for review: " + sow.getSowName();
        emailExecutor.execute(() -> sendMessage(sowId, recipient, subject, pdf));
    }

    private String validateEmail(String value) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidOperationException("Recipient email is required");
        }
        String email = value.trim();
        try {
            InternetAddress address = new InternetAddress(email, true);
            address.validate();
            if (!email.equals(address.getAddress())) {
                throw new InvalidOperationException("A single valid recipient email is required");
            }
        } catch (jakarta.mail.internet.AddressException exception) {
            throw new InvalidOperationException("A valid recipient email is required");
        }
        return email;
    }

    private byte[] readPdf(MultipartFile attachment) {
        if (attachment == null || attachment.isEmpty()) {
            throw new InvalidOperationException("A PDF attachment is required");
        }
        if (attachment.getSize() > MAX_ATTACHMENT_SIZE_BYTES) {
            throw new InvalidOperationException("PDF attachments must not exceed 10 MB");
        }

        final byte[] contents;
        try {
            contents = attachment.getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read the uploaded SOW PDF", exception);
        }
        if (contents.length > MAX_ATTACHMENT_SIZE_BYTES) {
            throw new InvalidOperationException("PDF attachments must not exceed 10 MB");
        }
        if (!hasPdfHeader(contents)) {
            throw new InvalidOperationException("The attachment must be a valid PDF");
        }
        return contents;
    }

    private boolean hasPdfHeader(byte[] contents) {
        int searchLength = Math.min(contents.length, 1024);
        for (int i = 0; i <= searchLength - PDF_HEADER.length; i++) {
            if (Arrays.equals(PDF_HEADER, Arrays.copyOfRange(contents, i, i + PDF_HEADER.length))) {
                return true;
            }
        }
        return false;
    }

    private void sendMessage(Long sowId, String recipient, String subject, byte[] pdf) {
        try {
            JavaMailSender mailSender = mailSenders.getObject();
            MimeMessageHelper message = new MimeMessageHelper(
                    mailSender.createMimeMessage(), true, StandardCharsets.UTF_8.name());
            if (StringUtils.hasText(fromAddress)) {
                message.setFrom(fromAddress);
            }
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText("Please find the SOW document attached for your review.");
            message.addAttachment("SOW-" + sowId + ".pdf", new ByteArrayResource(pdf), "application/pdf");
            mailSender.send(message.getMimeMessage());
        } catch (MessagingException | MailException exception) {
            log.error("SOW PDF email delivery failed for SOW {} ({})",
                    sowId, exception.getClass().getSimpleName());
        }
    }
}
