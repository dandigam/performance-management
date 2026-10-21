package com.rit.performance.service;

import com.rit.performance.controller.OnboardingBankDetailsController;
import com.rit.performance.dto.OnboardingBankDetailsRequest;
import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import com.rit.performance.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OnboardingBankDetailsServiceTest {
    @Test void savesOwnPrimaryAccountAndReturnsSavedBankDetails() throws Exception {
        var current = mock(CurrentEmployeeService.class);
        var banks = mock(BankAccountRepository.class);
        var onboardings = mock(EmployeeOnboardingRepository.class);
        var employee = new Employee(); employee.setId(101L); employee.setStatus("PENDING");
        var onboarding = new EmployeeOnboarding(); onboarding.setRequiredSections("[\"bank-details\"]");
        when(current.currentEmployee()).thenReturn(employee);
        when(onboardings.findForUpdateByEmployeeId(101L)).thenReturn(Optional.of(onboarding));
        when(onboardings.findByEmployeeId(101L)).thenReturn(Optional.of(onboarding));
        when(banks.saveAndFlush(any())).thenAnswer(call -> {
            BankAccount account = call.getArgument(0); account.setId(20L);
            when(banks.findFirstByOwnerTypeAndOwnerIdAndIsPrimaryTrueAndActiveTrue(
                    BankAccountOwnerType.EMPLOYEE, 101L)).thenReturn(Optional.of(account));
            return account;
        });
        var self = new OnboardingSelfService(current, onboardings, mock(LookupValueRepository.class),
                mock(EmployeeAddressRepository.class), mock(EmployeeEducationRepository.class),
                mock(EmployeeExperienceRepository.class), banks, mock(EmailNotificationService.class), mock(com.rit.performance.repository.UserRepository.class));
        var service = new OnboardingBankDetailsService(current, banks, onboardings, self);
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingBankDetailsController(service)).build();
        String payload = """
            {"bankCountry":"India","currency":"INR","accountHolderName":"Venkatesh ",
            "bankName":"SBI","accountNumber":"1234567890123","bankCode":"HDGC2748483"}
            """;
        for (int i = 0; i < 2; i++) {
            mvc.perform(put("/api/v1/onboarding/me/bank-details").param("employeeId", "999")
                    .contentType("application/json").content(payload))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.completedSections[0]").value("bank-details"))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.details.accountHolderName").value("Venkatesh"))
                    .andExpect(jsonPath("$.details.bankCode").value("HDGC2748483"))
                    .andExpect(jsonPath("$.details.accountNumber").value("1234567890123"));
        }
        var saved = org.mockito.ArgumentCaptor.forClass(BankAccount.class);
        verify(banks, times(2)).saveAndFlush(saved.capture());
        assertThat(saved.getAllValues().get(0)).isSameAs(saved.getAllValues().get(1));
        assertThat(saved.getValue().getOwnerId()).isEqualTo(101L);
        assertThat(saved.getValue().getOwnerType()).isEqualTo(BankAccountOwnerType.EMPLOYEE);
        assertThat(saved.getValue().getAccountNumberLast4()).isEqualTo("0123");
        assertThat(employee.getStatus()).isEqualTo("PENDING");
        var usa = new OnboardingBankDetailsRequest("USA", "USD", "Name", "Bank", "987654321", "123456789");
        service.update(usa);
        assertThat(saved.getValue().getIfscCode()).isNull();
        assertThat(saved.getValue().getRoutingNumberEncrypted()).isEqualTo("123456789");
        for (String state : List.of("SUBMITTED", "APPROVED")) {
            onboarding.setStatus(state);
            assertThatThrownBy(() -> service.update(usa)).isInstanceOf(ApplicationException.class);
        }
        when(onboardings.findForUpdateByEmployeeId(101L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(usa)).isInstanceOf(ApplicationException.class);
        verify(banks, times(3)).saveAndFlush(any());
        verify(banks, never()).findFirstByOwnerTypeAndOwnerIdAndIsPrimaryTrueAndActiveTrue(any(), eq(999L));
    }

    @Test void rejectsInvalidPayloadBeforeWriting() throws Exception {
        var service = mock(OnboardingBankDetailsService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingBankDetailsController(service)).build();
        for (String payload : List.of("{}", "{bad",
                """
                {"bankCountry":"USA","currency":"USD","accountHolderName":"Name","bankName":"Bank",
                "accountNumber":"12345678","bankCode":"invalid"}
                """))
            mvc.perform(put("/api/v1/onboarding/me/bank-details").contentType("application/json").content(payload))
                    .andExpect(status().is(422));
        verifyNoInteractions(service);
    }
}
