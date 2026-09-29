package com.rit.performance.service;

import com.rit.performance.controller.OnboardingPersonalController;
import com.rit.performance.dto.OnboardingPersonalRequest;
import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import com.rit.performance.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OnboardingPersonalServiceTest {
    final CurrentEmployeeService current = mock(CurrentEmployeeService.class);
    final EmployeeRepository employees = mock(EmployeeRepository.class);
    final EmployeeOnboardingRepository onboardings = mock(EmployeeOnboardingRepository.class);
    final OnboardingSelfService self = mock(OnboardingSelfService.class);
    final OnboardingPersonalService service = new OnboardingPersonalService(current, employees, onboardings, self);
    final OnboardingPersonalRequest request = new OnboardingPersonalRequest("1234567891", "MALE", LocalDate.of(1989, 1, 1));

    @Test void savesToSessionEmployeeAndStartsOnboarding() throws Exception {
        Employee employee = new Employee(); employee.setId(101L); employee.setStatus("PENDING");
        EmployeeOnboarding onboarding = new EmployeeOnboarding();
        when(current.currentEmployee()).thenReturn(employee);
        when(onboardings.findForUpdateByEmployeeId(101L)).thenReturn(Optional.of(onboarding));
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingPersonalController(service)).build();
        mvc.perform(put("/api/v1/onboarding/me/personal").param("employeeId", "999")
                .contentType("application/json").content("{\"phoneNumber\":\"1234567891\",\"gender\":\"MALE\",\"dateOfBirth\":\"1989-01-01\"}"))
                .andExpect(status().isOk());
        assertThat(employee.getPhoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(employee.getDateOfBirth()).isEqualTo(request.dateOfBirth());
        assertThat(employee.getStatus()).isEqualTo("PENDING");
        assertThat(onboarding.getStatus()).isEqualTo("IN_PROGRESS");
        verify(employees).saveAndFlush(employee);
        verify(onboardings, never()).findForUpdateByEmployeeId(999L);
    }

    @Test void rejectsSubmittedOnboardingAndDuplicatePhoneWithoutChangingEmployee() {
        Employee employee = new Employee(); employee.setId(101L);
        EmployeeOnboarding onboarding = new EmployeeOnboarding(); onboarding.setStatus("SUBMITTED");
        when(current.currentEmployee()).thenReturn(employee);
        when(onboardings.findForUpdateByEmployeeId(101L)).thenReturn(Optional.of(onboarding));
        assertThatThrownBy(() -> service.update(request)).isInstanceOf(ApplicationException.class);
        onboarding.setStatus("INVITED");
        when(employees.existsByPhoneNumberAndIdNot(request.phoneNumber(), 101L)).thenReturn(true);
        assertThatThrownBy(() -> service.update(request)).isInstanceOf(ApplicationException.class);
        assertThat(employee.getPhoneNumber()).isNull();
        verify(employees, never()).saveAndFlush(any());
    }

    @Test void rejectsMissingFields() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingPersonalController(service)).build();
        mvc.perform(put("/api/v1/onboarding/me/personal").contentType("application/json").content("{}"))
                .andExpect(status().is(422));
        verifyNoInteractions(current);
    }
}
