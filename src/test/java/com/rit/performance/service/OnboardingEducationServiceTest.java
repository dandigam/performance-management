package com.rit.performance.service;

import com.rit.performance.controller.OnboardingEducationController;
import com.rit.performance.dto.OnboardingEducationRequest;
import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import com.rit.performance.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OnboardingEducationServiceTest {
    @Test void replacesOwnEducationAndReportsCompletion() throws Exception {
        var current = mock(CurrentEmployeeService.class);
        var educations = mock(EmployeeEducationRepository.class);
        var onboardings = mock(EmployeeOnboardingRepository.class);
        var employee = new Employee(); employee.setId(101L); employee.setStatus("PENDING");
        var onboarding = new EmployeeOnboarding(); onboarding.setRequiredSections("[\"education\"]");
        when(current.currentEmployee()).thenReturn(employee);
        when(onboardings.findForUpdateByEmployeeId(101L)).thenReturn(Optional.of(onboarding));
        when(onboardings.findByEmployeeId(101L)).thenReturn(Optional.of(onboarding));
        when(educations.saveAllAndFlush(any())).thenAnswer(call -> {
            List<EmployeeEducation> saved = call.getArgument(0);
            assertThat(saved).hasSize(2).allSatisfy(row -> assertThat(row.getEmployee()).isSameAs(employee));
            assertThat(saved.get(0).getEducationType()).isEqualTo("B.Tech.");
            assertThat(saved.get(0).getCollegeUniversity()).isEqualTo("PITS");
            assertThat(saved.get(0).getPassingYear()).isEqualTo(2016);
            assertThat(saved.get(0).getPercentage()).isEqualByComparingTo("99");
            when(educations.findByEmployeeIdOrderByPassingYearDescIdDesc(101L)).thenReturn(saved);
            return saved;
        });
        var self = new OnboardingSelfService(current, onboardings, mock(LookupValueRepository.class),
                mock(EmployeeAddressRepository.class), educations, mock(EmployeeExperienceRepository.class), mock(BankAccountRepository.class), mock(EmailNotificationService.class), mock(com.rit.performance.repository.UserRepository.class));
        var service = new OnboardingEducationService(current, educations, onboardings, self);
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingEducationController(service)).build();
        String payload = """
            {"educationDetails":[{"degree":" B.Tech. ","institution":"PITS","passingYear":2016,"percentage":99},
            {"degree":"M.A.","institution":"NITS","passingYear":2012,"percentage":78}]}
            """;
        for (int i = 0; i < 2; i++) {
            mvc.perform(put("/api/v1/onboarding/me/education").param("employeeId", "999")
                    .contentType("application/json").content(payload))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.completedSections[0]").value("education"))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }
        verify(educations, times(2)).deleteByEmployeeId(101L);
        verify(educations, never()).deleteByEmployeeId(999L);
        assertThat(employee.getStatus()).isEqualTo("PENDING");
        var request = new OnboardingEducationRequest(List.of(new OnboardingEducationRequest.EducationDetail(
                "B.Tech.", "PITS", 2016, new BigDecimal("99"))));
        for (String status : List.of("SUBMITTED", "APPROVED")) {
            onboarding.setStatus(status);
            assertThatThrownBy(() -> service.update(request)).isInstanceOfSatisfying(ApplicationException.class,
                    error -> assertThat(error.getStatus().value()).isEqualTo(409));
        }
        when(onboardings.findForUpdateByEmployeeId(101L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(request)).isInstanceOfSatisfying(ApplicationException.class,
                error -> assertThat(error.getStatus().value()).isEqualTo(404));
        verify(educations, times(2)).saveAllAndFlush(any());
        verify(educations, times(2)).deleteByEmployeeId(anyLong());
    }

    @Test void rejectsInvalidPayloadBeforeWriting() throws Exception {
        var service = mock(OnboardingEducationService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingEducationController(service)).build();
        for (String payload : List.of("{}", "{bad", "{\"educationDetails\":[]}",
                "{\"educationDetails\":[null]}", "{\"educationDetails\":[{}]}",
                """
                {"educationDetails":[{"degree":"B.Tech.","institution":"PITS","passingYear":2016,"percentage":101}]}
                """,
                """
                {"educationDetails":[{"degree":" ","institution":"PITS","passingYear":2016,"percentage":99}]}
                """))
            mvc.perform(put("/api/v1/onboarding/me/education").contentType("application/json").content(payload))
                    .andExpect(status().is(422));
        verifyNoInteractions(service);
    }
}
