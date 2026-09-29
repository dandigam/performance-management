package com.rit.performance.service;

import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import com.rit.performance.exception.ApplicationException;
import com.rit.performance.controller.OnboardingSelfController;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OnboardingSelfServiceTest {
    @Test void getReturnsSessionEmployeesLiveRecordAndIgnoresSuppliedEmployeeId() throws Exception {
        var current = mock(CurrentEmployeeService.class);
        var repository = mock(EmployeeOnboardingRepository.class);
        var employee = new Employee(); employee.setId(101L); employee.setFirstName("Venkat");
        employee.setEmail("venkat@example.com"); employee.setStatus("PENDING");
        employee.setEmploymentType("FULL_TIME");
        when(current.currentEmployee()).thenReturn(employee);
        var onboarding = new EmployeeOnboarding(); onboarding.setEmployee(employee);
        onboarding.setRequiredSections("[\"personal\",\"documents\"]"); onboarding.setVersion(1);
        onboarding.setOriginalResponse("{\"invitationStatus\":\"QUEUED\"}");
        onboarding.setInvitationNotification(EmailNotification.builder().status(EmailDeliveryStatus.SENT).build());
        when(repository.findByEmployeeId(101L)).thenReturn(Optional.of(onboarding));
        var experiences = mock(EmployeeExperienceRepository.class);
        when(experiences.findByEmployeeIdOrderByFromDateDescIdDesc(101L)).thenReturn(java.util.List.of(
                EmployeeExperience.builder().employee(employee).companyName("Napier").position("Sr Java")
                        .location("Hyderabad").fromDate(java.time.LocalDate.of(2016, 1, 1))
                        .endDate(java.time.LocalDate.of(2017, 1, 1)).build()));
        var users = mock(UserRepository.class);
        var user = new User();
        user.setRole(LookupValue.builder().name("Employee").build());
        when(users.findByEmployeeId(101L)).thenReturn(Optional.of(user));
        employee.setWorkLocation("REMOTE");
        onboarding.setReviewComments("Please update details");
        onboarding.setReviewedAt(java.time.Instant.parse("2026-09-29T12:00:00Z"));
        var banks = mock(BankAccountRepository.class);
        var bank = new BankAccount(); bank.setCurrency("INR");
        when(banks.findFirstByOwnerTypeAndOwnerIdAndIsPrimaryTrueAndActiveTrue(
                BankAccountOwnerType.EMPLOYEE, 101L)).thenReturn(Optional.of(bank));
        var service = new OnboardingSelfService(current, repository, mock(LookupValueRepository.class), mock(EmployeeAddressRepository.class), mock(EmployeeEducationRepository.class), experiences, banks, mock(EmailNotificationService.class), users);
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingSelfController(service)).build();
        mvc.perform(get("/api/v1/onboarding/me").param("employeeId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.employeeId").value(101))
                .andExpect(jsonPath("$.invitationStatus").value("SENT"))
                .andExpect(jsonPath("$.employmentType").value("FULL_TIME"))
                .andExpect(jsonPath("$.roleName").value("Employee"))
                .andExpect(jsonPath("$.workLocation").value("REMOTE"))
                .andExpect(jsonPath("$.details.bankCurrency").value("INR"))
                .andExpect(jsonPath("$.details.currency").value("INR"))
                .andExpect(jsonPath("$.reviewComments").value("Please update details"))
                .andExpect(jsonPath("$.review.comments").value("Please update details"))
                .andExpect(jsonPath("$.requiredSections[0]").value("personal"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.details.employmentRecords.length()").value(1))
                .andExpect(jsonPath("$.details.employmentRecords[0].companyName").value("Napier"))
                .andExpect(jsonPath("$.details.employmentRecords[0].position").value("Sr Java"))
                .andExpect(jsonPath("$.details.employmentRecords[0].location").value("Hyderabad"))
                .andExpect(jsonPath("$.details.employmentRecords[0].fromDate").value("2016-01-01"))
                .andExpect(jsonPath("$.details.employmentRecords[0].endDate").value("2017-01-01"))
                .andExpect(jsonPath("$.originalResponse").doesNotExist())
                .andExpect(jsonPath("$.invitationToken").doesNotExist());
        verify(repository).findByEmployeeId(101L);
        verify(repository, never()).findByEmployeeId(999L);
        verify(experiences, never()).findByEmployeeIdOrderByFromDateDescIdDesc(999L);
        assertThat(employee.getStatus()).isEqualTo("PENDING");
        employee.setEmploymentType("CONTRACT");
        assertThat(service.responseFor(employee, onboarding, true).employmentType()).isEqualTo("CONTRACT");
        employee.setEmploymentType(null);
        assertThat(service.responseFor(employee, onboarding, true).employmentType()).isNull();
    }

    @Test void returnsDocumentTypesForTheEmployeesWorkMode() {
        var current = mock(CurrentEmployeeService.class);
        var repository = mock(EmployeeOnboardingRepository.class);
        var lookups = mock(LookupValueRepository.class);
        var employee = new Employee(); employee.setId(101L); employee.setFirstName("Test");
        when(current.currentEmployee()).thenReturn(employee);
        var onboarding = new EmployeeOnboarding(); onboarding.setRequiredSections("[\"documents\"]");
        when(repository.findByEmployeeId(101L)).thenReturn(Optional.of(onboarding));
        var service = new OnboardingSelfService(current, repository, lookups,
                mock(EmployeeAddressRepository.class), mock(EmployeeEducationRepository.class), mock(EmployeeExperienceRepository.class), mock(BankAccountRepository.class), mock(EmailNotificationService.class), mock(com.rit.performance.repository.UserRepository.class));
        for (String mode : java.util.List.of("ONSITE", "OFFSHORE")) {
            employee.setWorkMode(mode);
            var type = LookupValue.builder().id(1L).code("RESUME").name(mode + " Resume")
                    .requirementType("REQUIRED").build();
            when(lookups.findByLookupTypeCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrueOrderByDisplayOrderAscIdAsc(
                    "EMPLOYEE_ONBOARDING_DOCUMENTS_" + mode)).thenReturn(java.util.List.of(type));
            assertThat(service.getMine().documentTypes()).hasSize(1);
            assertThat(service.getMine().documentTypes().get(0))
                    .containsEntry("name", mode + " Resume").containsEntry("requirementType", "OPTIONAL");
            assertThat(service.getMine().completedSections()).contains("documents");
        }
        employee.setWorkMode(null);
        assertThat(service.getMine().documentTypes()).isEmpty();
    }

    @Test void missingRecordReturnsSpecificNotFound() {
        var current = mock(CurrentEmployeeService.class);
        var employee = new Employee(); employee.setId(101L);
        when(current.currentEmployee()).thenReturn(employee);
        var service = new OnboardingSelfService(current, mock(EmployeeOnboardingRepository.class), mock(LookupValueRepository.class), mock(EmployeeAddressRepository.class), mock(EmployeeEducationRepository.class), mock(EmployeeExperienceRepository.class), mock(BankAccountRepository.class), mock(EmailNotificationService.class), mock(com.rit.performance.repository.UserRepository.class));
        assertThatThrownBy(service::getMine).isInstanceOfSatisfying(ApplicationException.class, error -> {
            assertThat(error.getStatus().value()).isEqualTo(404);
            assertThat(error.getCode()).isEqualTo("ONBOARDING_NOT_FOUND");
        });
    }
}
