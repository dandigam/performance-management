package com.rit.performance.service;

import com.rit.performance.controller.OnboardingReviewController;
import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import com.rit.performance.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OnboardingReviewServiceTest {
    @Test void queueIncludesInvitedAndSubmittedAndReviewLoadsSelectedEmployee() throws Exception {
        var repository = mock(EmployeeOnboardingRepository.class);
        var current = mock(CurrentEmployeeService.class);
        var self = new OnboardingSelfService(current, repository, mock(LookupValueRepository.class),
                mock(EmployeeAddressRepository.class), mock(EmployeeEducationRepository.class),
                mock(EmployeeExperienceRepository.class), mock(BankAccountRepository.class), mock(EmailNotificationService.class), mock(com.rit.performance.repository.UserRepository.class));
        var service = new OnboardingReviewService(repository, self);
        var invited = record(10L, "INVITED");
        var submitted = record(11L, "SUBMITTED");
        submitted.setSubmittedAt(java.time.Instant.parse("2026-09-29T12:00:00Z"));
        when(repository.findQueue(isNull(), any())).thenReturn(new PageImpl<>(List.of(invited, submitted)));
        when(repository.findQueue(eq("SUBMITTED"), any())).thenReturn(new PageImpl<>(List.of(submitted)));
        when(repository.findByEmployeeId(11L)).thenReturn(Optional.of(submitted));
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingReviewController(service)).build();
        mvc.perform(get("/api/v1/hr/onboarding"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].status").value("INVITED"))
                .andExpect(jsonPath("$.content[1].status").value("SUBMITTED"))
                .andExpect(jsonPath("$.content[0].details").doesNotExist());
        mvc.perform(get("/api/v1/hr/onboarding").param("status", "submitted"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/v1/hr/onboarding/11"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.employeeId").value(11))
                .andExpect(jsonPath("$.details.educationRecords").isArray())
                .andExpect(jsonPath("$.submittedAt").value("2026-09-29T12:00:00Z"));
        verifyNoInteractions(current);
        verify(repository, never()).saveAndFlush(any());
        assertThatThrownBy(() -> service.review(999L)).isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> service.queue("unknown", 0, 20)).isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> service.queue(null, -1, 20)).isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> service.queue(null, 0, 101)).isInstanceOf(ApplicationException.class);
    }

    private EmployeeOnboarding record(long id, String status) {
        var employee = new Employee(); employee.setId(id); employee.setFirstName("Employee" + id);
        var onboarding = new EmployeeOnboarding(); onboarding.setEmployee(employee);
        onboarding.setStatus(status); onboarding.setRequiredSections("[\"personal\"]");
        return onboarding;
    }
}
