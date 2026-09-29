package com.rit.performance.service;

import com.rit.performance.controller.OnboardingAddressController;
import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OnboardingAddressServiceTest {
    @Test void createsThenUpdatesOwnAddressAndReportsCompletion() throws Exception {
        var current = mock(CurrentEmployeeService.class);
        var addresses = mock(EmployeeAddressRepository.class);
        var onboardings = mock(EmployeeOnboardingRepository.class);
        var employee = new Employee(); employee.setId(101L); employee.setStatus("PENDING");
        var onboarding = new EmployeeOnboarding(); onboarding.setRequiredSections("[\"personal\",\"address\"]");
        when(current.currentEmployee()).thenReturn(employee);
        when(onboardings.findForUpdateByEmployeeId(101L)).thenReturn(Optional.of(onboarding));
        when(onboardings.findByEmployeeId(101L)).thenReturn(Optional.of(onboarding));
        when(addresses.saveAndFlush(any())).thenAnswer(call -> {
            EmployeeAddress address = call.getArgument(0); address.setId(10L);
            when(addresses.findByEmployeeId(101L)).thenReturn(Optional.of(address));
            return address;
        });
        var self = new OnboardingSelfService(current, onboardings, mock(LookupValueRepository.class), addresses, mock(EmployeeEducationRepository.class), mock(EmployeeExperienceRepository.class), mock(BankAccountRepository.class), mock(EmailNotificationService.class), mock(com.rit.performance.repository.UserRepository.class));
        var service = new OnboardingAddressService(current, addresses, onboardings, self);
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingAddressController(service)).build();
        String payload = """
            {"addressLine1":" hyd ","addressLine2":"Nizampeta","city":"Hyderabad",
             "state":"Telangana","postalCode":"324243","country":"INDIA"}
            """;
        for (int i = 0; i < 2; i++) {
            mvc.perform(put("/api/v1/onboarding/me/address").param("employeeId", "999")
                    .contentType("application/json").content(payload))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.completedSections[0]").value("address"))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }
        var saved = org.mockito.ArgumentCaptor.forClass(EmployeeAddress.class);
        verify(addresses, times(2)).saveAndFlush(saved.capture());
        assertThat(saved.getAllValues().get(0)).isSameAs(saved.getAllValues().get(1));
        assertThat(saved.getValue().getEmployee()).isSameAs(employee);
        assertThat(saved.getValue().getAddressLine1()).isEqualTo("hyd");
        assertThat(employee.getStatus()).isEqualTo("PENDING");
        verify(addresses, never()).findByEmployeeId(999L);
        onboarding.setStatus("SUBMITTED");
        assertThatThrownBy(() -> service.update(new com.rit.performance.dto.OnboardingAddressRequest(
                "hyd", null, "Hyderabad", "Telangana", "324243", "INDIA")))
                .isInstanceOf(com.rit.performance.exception.ApplicationException.class);
        verify(addresses, times(2)).saveAndFlush(any());
    }

    @Test void rejectsInvalidPayloadBeforeWriting() throws Exception {
        var service = mock(OnboardingAddressService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingAddressController(service)).build();
        for (String payload : java.util.List.of("{}", "{bad"))
            mvc.perform(put("/api/v1/onboarding/me/address").contentType("application/json").content(payload))
                    .andExpect(status().is(422));
        verifyNoInteractions(service);
    }
}
