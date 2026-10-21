package com.rit.performance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rit.performance.dto.OnboardingCreateRequest;
import com.rit.performance.entity.*;
import com.rit.performance.exception.ApplicationException;
import com.rit.performance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OnboardingService {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    private static final List<String> SECTIONS = List.of("personal", "address", "education",
            "employment-history", "bank-details", "documents");
    private final EmployeeOnboardingRepository onboardings;
    private final EmployeeRepository employees;
    private final UserRepository users;
    private final LookupValueRepository lookups;
    private final VendorRepository vendors;
    private final PasswordResetTokenRepository tokens;
    private final EmailNotificationRepository emails;
    private final PasswordEncoder encoder;
    private final ApplicationEmailFactory emailFactory;
    private final PlatformTransactionManager transactionManager;
    private final Clock clock;
    @Value("${app.mail.base-url:http://localhost:5173}") private String frontendUrl;

    public String create(String actor, String key, OnboardingCreateRequest request) {
        if (key == null || !key.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
            throw invalid("Idempotency-Key must be a UUID.");
        String canonicalKey = UUID.fromString(key).toString();
        String hash = PasswordResetRateLimiter.hash(json(request));
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        try {
            return transaction.execute(status -> {
                var previous = onboardings.findByRequestActorAndIdempotencyKey(actor, canonicalKey);
                if (previous.isPresent()) return replay(previous.get(), hash);
                EmployeeOnboarding onboarding = new EmployeeOnboarding();
                onboarding.setRequestActor(actor);
                onboarding.setIdempotencyKey(canonicalKey);
                onboarding.setRequestHash(hash);
                onboarding.setRequiredSections(json(SECTIONS));
                // Reserve the unique key before employee creation; everything commits together.
                onboardings.saveAndFlush(onboarding);
                return createRecords(onboarding, request);
            });
        } catch (DataIntegrityViolationException exception) {
            // The failed transaction has rolled back. A concurrent winner can now be replayed.
            return transaction.execute(status -> {
                var winner = onboardings.findByRequestActorAndIdempotencyKey(actor, canonicalKey);
                if (winner.isPresent()) return replay(winner.get(), hash);
                if (employees.existsByEmailIgnoreCase(request.email().trim())
                        || users.existsByUsernameIgnoreCase(request.email().trim())) throw duplicateEmail();
                throw exception;
            });
        }
    }

    private String createRecords(EmployeeOnboarding onboarding, OnboardingCreateRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (employees.existsByEmailIgnoreCase(email) || users.existsByUsernameIgnoreCase(email))
            throw duplicateEmail();
        LookupValue role = lookup(request.roleId(), "SYSTEM_ROLE");
        LookupValue designation = lookup(request.designationId(), "DESIGNATION");
        String employmentType = code(request.employmentType(), "EMPLOYMENT_TYPE");
        String workMode = code(request.workMode(), "WORK_MODE");
        String workLocation = code(request.workLocation(), "WORK_LOCATION");
        if ("CONTRACT".equals(employmentType) && request.vendorId() == null)
            throw invalid("vendorId is required for CONTRACT employment.");
        Vendor vendor = request.vendorId() == null ? null : vendors.findById(request.vendorId())
                .orElseThrow(() -> invalid("vendorId does not exist."));

        Employee employee = new Employee();
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setEmail(email);
        employee.setJoiningDate(request.joiningDate());
        employee.setDesignationId(designation.getId());
        employee.setEmploymentType(employmentType);
        employee.setWorkMode(workMode);
        employee.setWorkLocation(workLocation);
        employee.setVendor(vendor);
        employee.setStatus("PENDING");
        employees.saveAndFlush(employee);
        employee.setRitId(String.format("RIT%02d", employee.getId()));
        employees.save(employee);

        User user = new User();
        user.setUsername(email);
        user.setPassword(encoder.encode(UUID.randomUUID().toString()));
        user.setEmployee(employee);
        user.setRole(role);
        user.setStatus("INVITED");
        user.setPortalAccess("ONBOARDING_ONLY");
        users.save(user);

        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(PasswordResetRateLimiter.hash(raw));
        token.setExpiresAt(clock.instant().plus(Duration.ofHours(24)));
        tokens.save(token);
        ApplicationEmail invitation = emailFactory.onboardingInvitation(employee, designation.getName(),
                frontendUrl.replaceAll("/$", "") + "/reset-password?token=" + raw);
        EmailNotification notification = EmailNotification.builder()
                .eventType(EmailEventType.ONBOARDING_INVITATION).recipientEmail(email)
                .recipientName(employee.getFirstName() + " " + employee.getLastName())
                .subject(invitation.subject()).body(invitation.body())
                .deduplicationKey("ONBOARDING_INVITATION:" + onboarding.getId()).build();
        emails.save(notification);
        onboarding.setEmployee(employee);
        onboarding.setInvitationToken(token);
        onboarding.setInvitationNotification(notification);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("employeeId", employee.getId());
        response.put("employeeNumber", employee.getRitId());
        response.put("employeeName", notification.getRecipientName());
        response.put("email", email);
        response.put("designationName", designation.getName());
        response.put("joiningDate", request.joiningDate().toString());
        response.put("status", "INVITED");
        response.put("completedSections", List.of());
        response.put("requiredSections", SECTIONS);
        response.put("invitationStatus", "QUEUED");
        response.put("invitationExpiresAt", token.getExpiresAt().toString());
        response.put("submittedAt", null);
        // Reserving the row inserts version 0; populating it increments to 1 at flush.
        // Expose the committed version, rather than the intermediate reservation version.
        response.put("version", onboarding.getVersion() + 1);
        onboarding.setOriginalResponse(json(response));
        onboardings.save(onboarding);
        return onboarding.getOriginalResponse();
    }

    private String replay(EmployeeOnboarding onboarding, String hash) {
        if (!hash.equals(onboarding.getRequestHash())) throw new ApplicationException(HttpStatus.CONFLICT,
                "IDEMPOTENCY_CONFLICT", "This idempotency key was already used with a different request.");
        return onboarding.getOriginalResponse();
    }

    private LookupValue lookup(Long id, String type) {
        LookupValue value = lookups.findById(id).orElseThrow(() -> invalid(type + " lookup does not exist."));
        if (!value.isActive() || value.getLookupType() == null || !value.getLookupType().isActive()
                || !type.equalsIgnoreCase(value.getLookupType().getCode()))
            throw invalid("An active " + type + " lookup is required.");
        return value;
    }

    private String code(String value, String type) {
        return lookups.findByLookupTypeCodeIgnoreCaseAndCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrue(
                type, value.trim()).orElseThrow(() -> invalid("Invalid " + type + "."))
                .getCode().toUpperCase(Locale.ROOT);
    }

    private static String json(Object value) {
        try { return JSON.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException("Cannot serialize onboarding data", exception); }
    }
    private static ApplicationException invalid(String message) {
        return new ApplicationException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_ONBOARDING_REQUEST", message);
    }
    private static ApplicationException duplicateEmail() {
        return new ApplicationException(HttpStatus.CONFLICT, "EMPLOYEE_EMAIL_EXISTS",
                "An employee or account with this email already exists.");
    }
}
