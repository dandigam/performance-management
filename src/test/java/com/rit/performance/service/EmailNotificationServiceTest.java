package com.rit.performance.service;

import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeReview;
import com.rit.performance.entity.EmployeeReviewAssessment;
import com.rit.performance.entity.EmailNotification;
import com.rit.performance.entity.PerformanceCycles;
import com.rit.performance.repository.EmailNotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotificationServiceTest {

    @Test
    void cyclePublishedEmailUsesGenericLoginUrl() {
        EmailNotificationRepository repository = mock(EmailNotificationRepository.class);
        EmailNotificationService service = new EmailNotificationService(repository);
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8081/");
        ReflectionTestUtils.setField(service, "defaultFooter", "Regards");

        Employee employee = new Employee();
        employee.setId(4L);
        employee.setFirstName("Dinakar");
        employee.setLastName("kalaga");
        employee.setEmail("dinakar@example.com");

        PerformanceCycles cycle = PerformanceCycles.builder()
                .id(10L)
                .cycleName("Testing")
                .build();
        EmployeeReview review = EmployeeReview.builder()
                .id(38L)
                .employee(employee)
                .performanceCycle(cycle)
                .build();

        when(repository.existsByDeduplicationKey(any())).thenReturn(false);
        service.queueCyclePublished(cycle, employee, review);

        var captor = org.mockito.ArgumentCaptor.forClass(EmailNotification.class);
        verify(repository).save(captor.capture());
        assertEquals("http://localhost:8081/login", captor.getValue().getActionUrl());
    }

    @Test
    void reopenedAssessmentEmailIncludesNewDueDateAndReason() {
        EmailNotificationRepository repository = mock(EmailNotificationRepository.class);
        EmailNotificationService service = new EmailNotificationService(repository);
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8081/");
        ReflectionTestUtils.setField(service, "defaultFooter", "Regards");

        Employee subject = new Employee();
        subject.setFirstName("Dinakar");
        subject.setEmail("dinakar@example.com");
        Employee reviewer = new Employee();
        reviewer.setFirstName("Venkatesh");
        reviewer.setEmail("venkatesh@example.com");
        EmployeeReview review = EmployeeReview.builder().id(38L).employee(subject)
                .performanceCycle(PerformanceCycles.builder().id(12L).cycleName("July Reviews").build())
                .build();
        EmployeeReviewAssessment assessment = EmployeeReviewAssessment.builder()
                .id(44L).employeeReview(review).assessorEmployee(reviewer).build();

        when(repository.existsByDeduplicationKey(any())).thenReturn(false);
        service.queueAssessmentReopened(review, assessment, LocalDate.of(2026, 8, 20),
                "Approved extension");

        var captor = org.mockito.ArgumentCaptor.forClass(EmailNotification.class);
        verify(repository).save(captor.capture());
        EmailNotification notification = captor.getValue();
        assertEquals(EmailEventType.ASSESSMENT_REOPENED, notification.getEventType());
        assertEquals("venkatesh@example.com", notification.getRecipientEmail());
        assertEquals("http://localhost:8081/login", notification.getActionUrl());
        assertTrue(notification.getBody().contains("2026-08-20"));
        assertTrue(notification.getBody().contains("Approved extension"));
    }
}
