package com.rit.performance.service;

import com.rit.performance.controller.OnboardingRequestChangesController;
import com.rit.performance.dto.OnboardingRequestChangesRequest;
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

class OnboardingRequestChangesServiceTest {
    @Test void recordsReviewQueuesEscapedEmailAndRejectsStaleOrDuplicateRequests() throws Exception {
        var repository = mock(EmployeeOnboardingRepository.class);
        var notifications = mock(EmailNotificationRepository.class);
        var current = mock(CurrentEmployeeService.class);
        var self = new OnboardingSelfService(current, repository, mock(LookupValueRepository.class),
                mock(EmployeeAddressRepository.class), mock(EmployeeEducationRepository.class),
                mock(EmployeeExperienceRepository.class), mock(BankAccountRepository.class), mock(EmailNotificationService.class), mock(com.rit.performance.repository.UserRepository.class));
        var factory = new ApplicationEmailFactory();
        org.springframework.test.util.ReflectionTestUtils.setField(factory, "frontendUrl", "https://example.test");
        var service = new OnboardingRequestChangesService(repository, self, factory, notifications);
        var employee = new Employee(); employee.setId(10L); employee.setFirstName("Employee");
        employee.setEmail("employee@example.test"); employee.setStatus("PENDING");
        var record = new EmployeeOnboarding(); record.setId(1L); record.setEmployee(employee);
        record.setStatus("SUBMITTED"); record.setVersion(4); record.setRequiredSections("[]");
        when(repository.findForUpdateByEmployeeId(10L)).thenReturn(Optional.of(record));
        when(repository.findByEmployeeId(10L)).thenReturn(Optional.of(record));
        when(repository.saveAndFlush(record)).thenAnswer(call -> { record.setVersion(5); return record; });
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingRequestChangesController(service)).build();
        mvc.perform(post("/api/v1/hr/onboarding/10/request-changes").principal(() -> "hr@example.test")
                .contentType("application/json").content("{\"version\":4,\"comments\":\" Fix dates <script> \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CHANGES_REQUESTED"))
                .andExpect(jsonPath("$.version").value(5))
                .andExpect(jsonPath("$.review.comments").value("Fix dates <script>"))
                .andExpect(jsonPath("$.review.reviewedBy").value("hr@example.test"))
                .andExpect(jsonPath("$.review.reviewedAt").isNotEmpty());
        when(current.currentEmployee()).thenReturn(employee);
        assertThat(self.getMine().review().comments()).isEqualTo("Fix dates <script>");
        var mail = org.mockito.ArgumentCaptor.forClass(EmailNotification.class);
        verify(notifications).save(mail.capture());
        assertThat(mail.getValue().getRecipientEmail()).isEqualTo(employee.getEmail());
        assertThat(mail.getValue().getBody()).contains("&lt;script&gt;", "resubmit", "https://example.test/login")
                .doesNotContain("<script>");
        assertThat(mail.getValue().getStatus()).isEqualTo(EmailDeliveryStatus.PENDING);
        assertThat(employee.getStatus()).isEqualTo("PENDING");
        assertThatThrownBy(() -> service.requestChanges(10L, new OnboardingRequestChangesRequest(4L, "again"), "hr"))
                .isInstanceOfSatisfying(ApplicationException.class, error -> assertThat(error.getCode()).isEqualTo("ONBOARDING_VERSION_CONFLICT"));
        assertThatThrownBy(() -> service.requestChanges(10L, new OnboardingRequestChangesRequest(5L, "again"), "hr"))
                .isInstanceOfSatisfying(ApplicationException.class, error -> assertThat(error.getCode()).isEqualTo("ONBOARDING_NOT_SUBMITTED"));
        verify(repository, times(1)).saveAndFlush(record);
        verify(notifications, times(1)).save(any());
    }

    @Test void invalidReviewPayloadNeverCallsService() throws Exception {
        var service = mock(OnboardingRequestChangesService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingRequestChangesController(service)).build();
        for (String body : java.util.List.of("{}", "{\"version\":4,\"comments\":\" \"}",
                "{\"version\":-1,\"comments\":\"fix\"}", "{bad"))
            mvc.perform(post("/api/v1/hr/onboarding/10/request-changes").principal(() -> "hr")
                    .contentType("application/json").content(body)).andExpect(status().is(422));
        verifyNoInteractions(service);
    }
}
