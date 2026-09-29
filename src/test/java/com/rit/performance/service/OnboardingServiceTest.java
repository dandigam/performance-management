package com.rit.performance.service;

import com.rit.performance.dto.OnboardingCreateRequest;
import com.rit.performance.entity.*;
import com.rit.performance.exception.ApplicationException;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class OnboardingServiceTest {
    EmployeeOnboardingRepository onboardings = mock(EmployeeOnboardingRepository.class);
    EmployeeRepository employees = mock(EmployeeRepository.class);
    UserRepository users = mock(UserRepository.class);
    LookupValueRepository lookups = mock(LookupValueRepository.class);
    VendorRepository vendors = mock(VendorRepository.class);
    PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
    EmailNotificationRepository emails = mock(EmailNotificationRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    ApplicationEmailFactory factory = mock(ApplicationEmailFactory.class);
    PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    String key = UUID.randomUUID().toString();
    OnboardingService service;
    OnboardingCreateRequest request = new OnboardingCreateRequest("Venkat", "D", "venkat@example.com",
            LocalDate.of(2026, 1, 1), 29L, 100L, "FULL_TIME", "OFFSHORE", "OFFICE", null);

    @BeforeEach void setup() {
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new OnboardingService(onboardings, employees, users, lookups, vendors, tokens, emails,
                encoder, factory, transactions, Clock.fixed(Instant.parse("2026-09-27T12:00:00Z"), ZoneOffset.UTC));
        ReflectionTestUtils.setField(service, "frontendUrl", "https://portal.example.com");
    }

    void validLookups() {
        when(lookups.findById(29L)).thenReturn(Optional.of(lookup(29L, "SYSTEM_ROLE", "EMPLOYEE")));
        when(lookups.findById(100L)).thenReturn(Optional.of(lookup(100L, "DESIGNATION", "Engineer")));
        for (String[] pair : List.of(new String[]{"EMPLOYMENT_TYPE", "FULL_TIME"},
                new String[]{"WORK_MODE", "OFFSHORE"}, new String[]{"WORK_LOCATION", "OFFICE"}))
            when(lookups.findByLookupTypeCodeIgnoreCaseAndCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrue(pair[0], pair[1]))
                    .thenReturn(Optional.of(lookup(1L, pair[0], pair[1])));
        when(onboardings.saveAndFlush(any())).thenAnswer(call -> {
            EmployeeOnboarding value = call.getArgument(0); value.setId(10L); return value;
        });
        when(employees.saveAndFlush(any())).thenAnswer(call -> {
            Employee value = call.getArgument(0); value.setId(101L); return value;
        });
        when(encoder.encode(any())).thenReturn("hashed-random-secret");
        when(factory.onboardingInvitation(any(Employee.class), anyString(), anyString()))
                .thenReturn(new ApplicationEmail("venkat@example.com", "Invite", "Setup link", true));
    }

    @Test void createsPendingEmployeeAndDurableInvitationThenReplaysWithoutSendingAgain() {
        validLookups();
        String response = service.create("hr", key, request);
        assertThat(response).contains("\"employeeNumber\":\"RIT101\"", "\"version\":1",
                "\"invitationStatus\":\"QUEUED\"", "2026-09-28T12:00:00Z");
        var employee = ArgumentCaptor.forClass(Employee.class);
        verify(employees).saveAndFlush(employee.capture());
        assertThat(employee.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(employee.getValue().getPhoneNumber()).isNull();
        var user = ArgumentCaptor.forClass(User.class);
        verify(users).save(user.capture());
        assertThat(user.getValue().getPortalAccess()).isEqualTo("ONBOARDING_ONLY");
        assertThat(user.getValue().getStatus()).isEqualTo("INVITED");
        var token = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).save(token.capture());
        assertThat(token.getValue().getTokenHash()).hasSize(64);
        assertThat(token.getValue().isUsed()).isFalse();
        var record = ArgumentCaptor.forClass(EmployeeOnboarding.class);
        verify(onboardings).save(record.capture());
        when(onboardings.findByRequestActorAndIdempotencyKey("hr", key)).thenReturn(Optional.of(record.getValue()));
        assertThat(service.create("hr", key, request)).isEqualTo(response);
        verify(emails, times(1)).save(any());
        verify(employees, times(1)).saveAndFlush(any());
    }

    @Test void rejectsChangedRequestForSameKey() {
        EmployeeOnboarding existing = new EmployeeOnboarding(); existing.setRequestHash("different");
        when(onboardings.findByRequestActorAndIdempotencyKey("hr", key)).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.create("hr", key, request)).isInstanceOfSatisfying(ApplicationException.class,
                ex -> assertThat(ex.getCode()).isEqualTo("IDEMPOTENCY_CONFLICT"));
        verifyNoInteractions(employees, users, emails);
    }

    @Test void concurrentKeyCollisionReplaysWinnerAfterRollback() {
        validLookups();
        String original = service.create("hr", key, request);
        var captured = ArgumentCaptor.forClass(EmployeeOnboarding.class);
        verify(onboardings).save(captured.capture());
        when(onboardings.findByRequestActorAndIdempotencyKey("hr", key))
                .thenReturn(Optional.empty()).thenReturn(Optional.of(captured.getValue()));
        doThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"))
                .when(onboardings).saveAndFlush(any());
        assertThat(service.create("hr", key, request)).isEqualTo(original);
        verify(transactions).rollback(any());
        verify(emails, times(1)).save(any());
    }

    @Test void rejectsMissingKeyBeforeAnyWrites() {
        assertThatThrownBy(() -> service.create("hr", null, request)).isInstanceOf(ApplicationException.class);
        verifyNoInteractions(onboardings, employees);
    }

    @Test void rejectsExistingAccount() {
        when(users.existsByUsernameIgnoreCase(request.email())).thenReturn(true);
        assertThatThrownBy(() -> service.create("hr", key, request)).isInstanceOfSatisfying(ApplicationException.class,
                ex -> assertThat(ex.getCode()).isEqualTo("EMPLOYEE_EMAIL_EXISTS"));
        verify(employees, never()).saveAndFlush(any());
        verify(transactions).rollback(any());
    }

    @Test void contractRequiresVendor() {
        validLookups();
        when(lookups.findByLookupTypeCodeIgnoreCaseAndCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrue("EMPLOYMENT_TYPE", "CONTRACT"))
                .thenReturn(Optional.of(lookup(1L, "EMPLOYMENT_TYPE", "CONTRACT")));
        var contract = new OnboardingCreateRequest("Venkat", "D", request.email(), request.joiningDate(),
                29L, 100L, "CONTRACT", "OFFSHORE", "OFFICE", null);
        assertThatThrownBy(() -> service.create("hr", key, contract)).isInstanceOfSatisfying(ApplicationException.class,
                ex -> assertThat(ex.getStatus().value()).isEqualTo(422));
        verify(employees, never()).saveAndFlush(any());
    }

    private LookupValue lookup(Long id, String type, String code) {
        return LookupValue.builder().id(id).code(code).name(code).active(true)
                .lookupType(LookupType.builder().code(type).active(true).build()).build();
    }
}
