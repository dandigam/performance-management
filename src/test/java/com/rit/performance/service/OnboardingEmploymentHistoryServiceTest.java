package com.rit.performance.service;

import com.rit.performance.controller.OnboardingEmploymentHistoryController;
import com.rit.performance.dto.OnboardingEmploymentHistoryRequest;
import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import com.rit.performance.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OnboardingEmploymentHistoryServiceTest {
    @Test void replacesOwnHistoryAndReportsCompletion() throws Exception {
        var current = mock(CurrentEmployeeService.class);
        var experiences = mock(EmployeeExperienceRepository.class);
        var onboardings = mock(EmployeeOnboardingRepository.class);
        var employee = new Employee(); employee.setId(101L); employee.setStatus("PENDING");
        var onboarding = new EmployeeOnboarding(); onboarding.setRequiredSections("[\"employment-history\"]");
        when(current.currentEmployee()).thenReturn(employee);
        when(onboardings.findForUpdateByEmployeeId(101L)).thenReturn(Optional.of(onboarding));
        when(onboardings.findByEmployeeId(101L)).thenReturn(Optional.of(onboarding));
        when(experiences.saveAllAndFlush(any())).thenAnswer(call -> {
            List<EmployeeExperience> saved = call.getArgument(0);
            assertThat(saved).hasSize(2).allSatisfy(row -> assertThat(row.getEmployee()).isSameAs(employee));
            assertThat(saved.get(0).getCompanyName()).isEqualTo("Napier");
            assertThat(saved.get(0).getPosition()).isEqualTo("Sr Java");
            assertThat(saved.get(0).getLocation()).isEqualTo("Hyderabad");
            assertThat(saved.get(0).getFromDate()).isEqualTo(LocalDate.of(2016, 1, 1));
            assertThat(saved.get(0).getEndDate()).isEqualTo(LocalDate.of(2017, 1, 1));
            assertThat(saved.get(1).getEndDate()).isNull();
            when(experiences.findByEmployeeIdOrderByFromDateDescIdDesc(101L)).thenReturn(saved);
            return saved;
        });
        var self = new OnboardingSelfService(current, onboardings, mock(LookupValueRepository.class),
                mock(EmployeeAddressRepository.class), mock(EmployeeEducationRepository.class), experiences, mock(BankAccountRepository.class), mock(EmailNotificationService.class), mock(com.rit.performance.repository.UserRepository.class));
        var service = new OnboardingEmploymentHistoryService(current, experiences, onboardings, self);
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingEmploymentHistoryController(service)).build();
        String payload = """
            {"experienceDetails":[{"companyName":" Napier ","position":"Sr Java","location":"Hyderabad",
            "fromDate":"2016-01-01","endDate":"2017-01-01"},
            {"companyName":"Accenture","position":"SR Java","location":"Hyderabad","fromDate":"2018-01-01","endDate":null}]}
            """;
        for (int i = 0; i < 2; i++) {
            mvc.perform(put("/api/v1/onboarding/me/employment-history").param("employeeId", "999")
                    .contentType("application/json").content(payload))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.completedSections[0]").value("employment-history"))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }
        // A fresh GET must restore every saved row, including an open-ended role.
        var reload = MockMvcBuilders.standaloneSetup(
                new com.rit.performance.controller.OnboardingSelfController(self)).build();
        reload.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/onboarding/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.employmentRecords.length()").value(2))
                .andExpect(jsonPath("$.details.employmentRecords[0].companyName").value("Napier"))
                .andExpect(jsonPath("$.details.employmentRecords[0].fromDate").value("2016-01-01"))
                .andExpect(jsonPath("$.details.employmentRecords[0].endDate").value("2017-01-01"))
                .andExpect(jsonPath("$.details.employmentRecords[1].companyName").value("Accenture"))
                .andExpect(jsonPath("$.details.employmentRecords[1].endDate").value(""));
        verify(experiences, times(2)).deleteByEmployeeId(101L);
        verify(experiences, never()).deleteByEmployeeId(999L);
        assertThat(employee.getStatus()).isEqualTo("PENDING");
        var request = new OnboardingEmploymentHistoryRequest(List.of(new OnboardingEmploymentHistoryRequest.ExperienceDetail(
                "Napier", "Sr Java", "Hyderabad", LocalDate.of(2016, 1, 1), null)));
        for (String status : List.of("SUBMITTED", "APPROVED")) {
            onboarding.setStatus(status);
            assertThatThrownBy(() -> service.update(request)).isInstanceOfSatisfying(ApplicationException.class,
                    error -> assertThat(error.getStatus().value()).isEqualTo(409));
        }
        when(onboardings.findForUpdateByEmployeeId(101L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(request)).isInstanceOfSatisfying(ApplicationException.class,
                error -> assertThat(error.getStatus().value()).isEqualTo(404));
        verify(experiences, times(2)).saveAllAndFlush(any());
        verify(experiences, times(2)).deleteByEmployeeId(anyLong());
    }

    @Test void rejectsInvalidPayloadAndReversedDatesBeforeWriting() throws Exception {
        var service = mock(OnboardingEmploymentHistoryService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingEmploymentHistoryController(service)).build();
        for (String payload : List.of("{}", "{bad", "{\"experienceDetails\":[]}",
                "{\"experienceDetails\":[null]}", "{\"experienceDetails\":[{}]}",
                """
                {"experienceDetails":[{"companyName":"Napier","position":"Sr Java","location":"Hyderabad","fromDate":"2016-01-01","endDate":"2015-01-01"},
                {"companyName":"Accenture","position":"SR Java","location":"Hyderabad","fromDate":"2016-01-01","endDate":"2014-01-02"}]}
                """))
            mvc.perform(put("/api/v1/onboarding/me/employment-history").contentType("application/json").content(payload))
                    .andExpect(status().is(422)).andExpect(jsonPath("$.code").value("INVALID_EMPLOYMENT_HISTORY"));
        verifyNoInteractions(service);
    }
}
