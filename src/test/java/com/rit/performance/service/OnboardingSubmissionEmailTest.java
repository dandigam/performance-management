package com.rit.performance.service;

import com.rit.performance.entity.*;
import com.rit.performance.repository.EmailNotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class OnboardingSubmissionEmailTest {
    @Test void queuesOneCategoryEmailWithSubmissionDetailsAndLabelsResubmission() {
        var repository = mock(EmailNotificationRepository.class);
        var factory = new ApplicationEmailFactory();
        ReflectionTestUtils.setField(factory, "frontendUrl", "https://example.test");
        var service = new EmailNotificationService(repository, factory);
        var employee = new Employee(); employee.setFirstName("Employee <test>"); employee.setRitId("RIT10");
        var onboarding = new EmployeeOnboarding(); onboarding.setId(10L); onboarding.setEmployee(employee);
        onboarding.setVersion(5); onboarding.setSubmittedAt(java.time.Instant.parse("2026-09-29T12:00:00Z"));
        service.queueOnboardingSubmitted(onboarding, false);
        var saved = org.mockito.ArgumentCaptor.forClass(EmailNotification.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getRecipientEmail()).isEmpty();
        assertThat(saved.getValue().getSubject()).contains("submitted for review", "RIT10");
        assertThat(saved.getValue().getBody()).contains("&lt;test&gt;", "2026-09-29T12:00:00Z", "https://example.test/login");
        assertThat(saved.getValue().getStatus()).isEqualTo(EmailDeliveryStatus.PENDING);
        String key = saved.getValue().getDeduplicationKey();
        when(repository.existsByDeduplicationKey(key)).thenReturn(true);
        service.queueOnboardingSubmitted(onboarding, false);
        verify(repository, times(1)).save(any());
        onboarding.setVersion(7);
        service.queueOnboardingSubmitted(onboarding, true);
        verify(repository, times(2)).save(saved.capture());
        assertThat(saved.getValue().getSubject()).contains("resubmitted for review");
        assertThat(saved.getValue().getDeduplicationKey()).isNotEqualTo(key);
    }
}
