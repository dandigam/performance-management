package com.rit.performance.service;

import com.rit.performance.controller.OnboardingSelfController;
import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import com.rit.performance.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OnboardingSubmitServiceTest {
    private final CurrentEmployeeService current = mock(CurrentEmployeeService.class);
    private final EmployeeOnboardingRepository onboardings = mock(EmployeeOnboardingRepository.class);
    private final Employee employee = new Employee();
    private final EmployeeOnboarding onboarding = new EmployeeOnboarding();
    private final EmailNotificationService notifications = mock(EmailNotificationService.class);
    private final OnboardingSelfService service = new OnboardingSelfService(current, onboardings,
            mock(LookupValueRepository.class), mock(EmployeeAddressRepository.class),
            mock(EmployeeEducationRepository.class), mock(EmployeeExperienceRepository.class), mock(BankAccountRepository.class), notifications, mock(com.rit.performance.repository.UserRepository.class));

    OnboardingSubmitServiceTest() {
        employee.setId(10L); employee.setStatus("PENDING");
        onboarding.setRequiredSections("[\"personal\",\"documents\"]");
        when(current.currentEmployee()).thenReturn(employee);
        when(onboardings.findForUpdateByEmployeeId(10L)).thenReturn(Optional.of(onboarding));
        when(onboardings.findByEmployeeId(10L)).thenReturn(Optional.of(onboarding));
    }

    @Test void submitsOwnSavedDetailsWithoutBodyAndRepeatedSubmissionPreservesTimestamp() throws Exception {
        employee.setPhoneNumber("1234567890"); employee.setGender("MALE");
        employee.setDateOfBirth(java.time.LocalDate.of(1990, 1, 1));
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingSelfController(service)).build();
        mvc.perform(post("/api/v1/onboarding/me/submit").param("employeeId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.submittedAt").isNotEmpty());
        var timestamp = onboarding.getSubmittedAt();
        service.submit();
        assertThat(onboarding.getSubmittedAt()).isEqualTo(timestamp);
        assertThat(employee.getStatus()).isEqualTo("PENDING");
        verify(onboardings, times(1)).saveAndFlush(onboarding);
        verify(notifications, times(1)).queueOnboardingSubmitted(onboarding, false);
        verify(onboardings, never()).findForUpdateByEmployeeId(999L);
        onboarding.setStatus("CHANGES_REQUESTED");
        assertThat(service.submit().status()).isEqualTo("SUBMITTED");
        verify(notifications).queueOnboardingSubmitted(onboarding, true);
    }

    @Test void incompleteRequiredSectionsAreReportedWithoutSaving() {
        onboarding.setRequiredSections("[\"personal\",\"address\",\"education\",\"employment-history\",\"bank-details\",\"documents\"]");
        assertThatThrownBy(service::submit).isInstanceOfSatisfying(ApplicationException.class, error -> {
            assertThat(error.getStatus().value()).isEqualTo(422);
            assertThat(error.getCode()).isEqualTo("ONBOARDING_INCOMPLETE");
            assertThat(error.getMessage()).contains("personal", "address", "education", "employment-history", "bank-details")
                    .doesNotContain("documents");
        });
        assertThat(onboarding.getStatus()).isEqualTo("INVITED");
        assertThat(onboarding.getSubmittedAt()).isNull();
        verifyNoInteractions(notifications);
        verify(onboardings, never()).saveAndFlush(any());
    }

    @Test void rejectsApprovedAndMissingOnboarding() {
        onboarding.setStatus("APPROVED");
        assertThatThrownBy(service::submit).isInstanceOfSatisfying(ApplicationException.class,
                error -> assertThat(error.getStatus().value()).isEqualTo(409));
        when(onboardings.findForUpdateByEmployeeId(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(service::submit).isInstanceOfSatisfying(ApplicationException.class,
                error -> assertThat(error.getStatus().value()).isEqualTo(404));
        verify(onboardings, never()).saveAndFlush(any());
    }
}
