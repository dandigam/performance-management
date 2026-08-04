package com.rit.performance.dto;

import com.rit.performance.service.EmailEventType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailNotificationRequest {
    private EmailEventType eventType = EmailEventType.MANUAL;
    @NotBlank @Email @Size(max = 150)
    private String recipientEmail;
    @Size(max = 120)
    private String recipientName;
    @NotBlank @Size(max = 250)
    private String subject;
    @NotBlank
    private String body;
    @Size(max = 500)
    private String footer;
    @Size(max = 1000)
    private String actionUrl;
    private Long employeeReviewId;
    private Long cycleId;
}
