package com.rit.performance.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rit.performance.dto.OnboardingResponse;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeOnboarding;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.exception.ApplicationException;
import com.rit.performance.repository.EmployeeOnboardingRepository;
import com.rit.performance.repository.LookupValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OnboardingSelfService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final CurrentEmployeeService currentEmployee;
    private final EmployeeOnboardingRepository onboardings;
    private final LookupValueRepository lookups;
    private final com.rit.performance.repository.EmployeeAddressRepository addresses;
    private final com.rit.performance.repository.EmployeeEducationRepository educations;
    private final com.rit.performance.repository.EmployeeExperienceRepository experiences;
    private final com.rit.performance.repository.BankAccountRepository banks;
    private final EmailNotificationService notifications;
    private final com.rit.performance.repository.UserRepository users;

    @Transactional
    public OnboardingResponse submit() {
        var employee = currentEmployee.currentEmployee();
        var onboarding = onboardings.findForUpdateByEmployeeId(employee.getId())
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "ONBOARDING_NOT_FOUND",
                        "No onboarding record exists for your employee account."));
        // Repeated submissions return the current summary without changing the timestamp.
        if ("SUBMITTED".equals(onboarding.getStatus())) return getMine();
        if (!java.util.Set.of("INVITED", "IN_PROGRESS", "CHANGES_REQUESTED").contains(onboarding.getStatus()))
            throw new ApplicationException(HttpStatus.CONFLICT, "ONBOARDING_NOT_EDITABLE",
                    "Onboarding cannot be submitted at this stage.");
        var completed = completedSections(employee);
        var missing = requiredSections(onboarding).stream().filter(section -> !completed.contains(section)).toList();
        if (!missing.isEmpty())
            throw new ApplicationException(HttpStatus.UNPROCESSABLE_ENTITY, "ONBOARDING_INCOMPLETE",
                    "Complete the required sections before submitting: " + String.join(", ", missing) + ".");
        boolean resubmission = "CHANGES_REQUESTED".equals(onboarding.getStatus());
        onboarding.setStatus("SUBMITTED");
        onboarding.setSubmittedAt(java.time.Instant.now());
        onboarding.setUpdatedOn(java.time.LocalDateTime.now());
        onboardings.saveAndFlush(onboarding);
        notifications.queueOnboardingSubmitted(onboarding, resubmission);
        return getMine();
    }

    @Transactional(readOnly = true)
    public OnboardingResponse getMine() {
        // Resolve ownership exclusively from the authenticated account, never request parameters.
        Employee employee = currentEmployee.currentEmployee();
        EmployeeOnboarding onboarding = onboardings.findByEmployeeId(employee.getId())
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "ONBOARDING_NOT_FOUND",
                        "No onboarding record exists for your employee account."));
        return responseFor(employee, onboarding);
    }

    OnboardingResponse responseFor(Employee employee, EmployeeOnboarding onboarding) {
        return responseFor(employee, onboarding, true);
    }

    OnboardingResponse responseFor(Employee employee, EmployeeOnboarding onboarding, boolean includeDetails) {
        String designation = employee.getDesignationId() == null ? null
                : lookups.findById(employee.getDesignationId()).map(LookupValue::getName).orElse(null);
        var notification = onboarding.getInvitationNotification();
        String delivery = notification == null || notification.getStatus() == null ? null
                : notification.getStatus() == EmailDeliveryStatus.PENDING ? "QUEUED" : notification.getStatus().name();
        return new OnboardingResponse(employee.getId(), employee.getRitId(),
                (employee.getFirstName() + " " + (employee.getLastName() == null ? "" : employee.getLastName())).trim(),
                employee.getEmail(), designation, employee.getJoiningDate(), onboarding.getStatus(),
                completedSections(employee), requiredSections(onboarding), delivery,
                onboarding.getInvitationToken() == null ? null : onboarding.getInvitationToken().getExpiresAt(),
                onboarding.getSubmittedAt(), onboarding.getVersion(), includeDetails ? profileDetails(employee) : null,
                includeDetails ? text(employee.getPhoneNumber()) : null, text(employee.getWorkMode()),
                includeDetails ? documentTypes(employee) : null,
                onboarding.getReviewedAt() == null ? null : new OnboardingResponse.Review(
                        onboarding.getReviewComments(), onboarding.getReviewedBy(), onboarding.getReviewedAt()),
                employee.getEmploymentType(), users.findByEmployeeId(employee.getId())
                        .map(com.rit.performance.entity.User::getRole).map(LookupValue::getName).orElse(null),
                employee.getWorkLocation());
    }

    private List<java.util.Map<String, Object>> documentTypes(Employee employee) {
        String mode = text(employee.getWorkMode()).trim().toUpperCase(java.util.Locale.ROOT);
        if (!List.of("ONSITE", "OFFSHORE").contains(mode)) return List.of();
        return lookups.findByLookupTypeCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrueOrderByDisplayOrderAscIdAsc(
                "EMPLOYEE_ONBOARDING_DOCUMENTS_" + mode).stream().map(value -> {
            java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", value.getId());
            item.put("code", value.getCode());
            item.put("name", value.getName());
            item.put("description", value.getDescription());
            item.put("displayOrder", value.getDisplayOrder());
            item.put("requirementType", "OPTIONAL");
            item.put("active", value.isActive());
            return item;
        }).toList();
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private java.util.Map<String, Object> profileDetails(Employee employee) {
        var details = new java.util.LinkedHashMap<String, Object>();
        details.put("gender", text(employee.getGender()));
        details.put("dateOfBirth", text(employee.getDateOfBirth()));
        addresses.findByEmployeeId(employee.getId()).ifPresent(address -> {
            details.put("addressLine1", text(address.getAddressLine1()));
            details.put("addressLine2", text(address.getAddressLine2()));
            details.put("city", text(address.getCity()));
            details.put("state", text(address.getState()));
            details.put("postalCode", text(address.getPostalCode()));
            details.put("country", text(address.getCountry()));
        });
        details.put("educationRecords", educations.findByEmployeeIdOrderByPassingYearDescIdDesc(employee.getId())
                .stream().map(row -> {
                    var item = new java.util.LinkedHashMap<String, Object>();
                    item.put("degree", text(row.getEducationType()));
                    item.put("institution", text(row.getCollegeUniversity()));
                    item.put("passingYear", row.getPassingYear());
                    item.put("percentage", row.getPercentage());
                    return item;
                }).toList());
        details.put("employmentRecords", experiences.findByEmployeeIdOrderByFromDateDescIdDesc(employee.getId())
                .stream().map(row -> java.util.Map.of(
                        "companyName", text(row.getCompanyName()),
                        "position", text(row.getPosition()),
                        "location", text(row.getLocation()),
                        "fromDate", text(row.getFromDate()),
                        "endDate", text(row.getEndDate()))).toList());
        banks.findFirstByOwnerTypeAndOwnerIdAndIsPrimaryTrueAndActiveTrue(
                com.rit.performance.entity.BankAccountOwnerType.EMPLOYEE, employee.getId()).ifPresent(bank -> {
            details.put("bankCountry", text(bank.getBankCountry()));
            details.put("currency", text(bank.getCurrency()));
            details.put("bankCurrency", text(bank.getCurrency()));
            details.put("accountHolderName", text(bank.getAccountHolderName()));
            details.put("bankName", text(bank.getBankName()));
            details.put("accountNumber", text(bank.getAccountNumberEncrypted()));
            details.put("bankCode", text("INDIA".equalsIgnoreCase(bank.getBankCountry())
                    ? bank.getIfscCode() : bank.getRoutingNumberEncrypted()));
        });
        var uploaded = new java.util.LinkedHashMap<String, java.util.List<java.util.Map<String, Object>>>();
        for (var link : employee.getEmployeeDocuments()) {
            if (!"ACTIVE".equals(link.getStatus()) || link.getDocumentType() == null) continue;
            var document = link.getDocument();
            var item = new java.util.LinkedHashMap<String, Object>();
            item.put("id", document.getId());
            item.put("documentName", document.getDocumentName());
            item.put("fileType", document.getFileType());
            item.put("module", document.getModule());
            item.put("uploadedAt", text(document.getUploadedAt()));
            item.put("documentType", link.getDocumentType().getCode());
            item.put("fileUrl", "/api/v1/documents/" + document.getId() + "/download");
            uploaded.computeIfAbsent(link.getDocumentType().getCode(), key -> new java.util.ArrayList<>()).add(item);
        }
        details.put("onboardingDocuments", uploaded);
        return details;
    }

    private List<String> completedSections(Employee employee) {
        var completed = new java.util.ArrayList<String>();
        if (employee.getPhoneNumber() != null && !employee.getPhoneNumber().isBlank()
                && employee.getGender() != null && !employee.getGender().isBlank()
                && employee.getDateOfBirth() != null) completed.add("personal");
        addresses.findByEmployeeId(employee.getId()).filter(address ->
                org.springframework.util.StringUtils.hasText(address.getAddressLine1())
                && org.springframework.util.StringUtils.hasText(address.getCity())
                && org.springframework.util.StringUtils.hasText(address.getState())
                && org.springframework.util.StringUtils.hasText(address.getPostalCode())
                && org.springframework.util.StringUtils.hasText(address.getCountry()))
                .ifPresent(address -> completed.add("address"));
        var educationDetails = educations.findByEmployeeIdOrderByPassingYearDescIdDesc(employee.getId());
        if (!educationDetails.isEmpty() && educationDetails.stream().allMatch(education ->
                org.springframework.util.StringUtils.hasText(education.getEducationType())
                && org.springframework.util.StringUtils.hasText(education.getCollegeUniversity())
                && education.getPassingYear() != null
                && education.getPercentage() != null)) completed.add("education");
        var experienceDetails = experiences.findByEmployeeIdOrderByFromDateDescIdDesc(employee.getId());
        if (!experienceDetails.isEmpty() && experienceDetails.stream().allMatch(experience ->
                org.springframework.util.StringUtils.hasText(experience.getCompanyName())
                && org.springframework.util.StringUtils.hasText(experience.getPosition())
                && org.springframework.util.StringUtils.hasText(experience.getLocation())
                && experience.getFromDate() != null
                && (experience.getEndDate() == null || !experience.getEndDate().isBefore(experience.getFromDate()))))
            completed.add("employment-history");
        banks.findFirstByOwnerTypeAndOwnerIdAndIsPrimaryTrueAndActiveTrue(
                com.rit.performance.entity.BankAccountOwnerType.EMPLOYEE, employee.getId())
                .filter(bank -> org.springframework.util.StringUtils.hasText(bank.getBankCountry())
                        && org.springframework.util.StringUtils.hasText(bank.getCurrency())
                        && org.springframework.util.StringUtils.hasText(bank.getAccountHolderName())
                        && org.springframework.util.StringUtils.hasText(bank.getBankName())
                        && org.springframework.util.StringUtils.hasText(bank.getAccountNumberEncrypted())
                        && org.springframework.util.StringUtils.hasText("INDIA".equalsIgnoreCase(bank.getBankCountry())
                                ? bank.getIfscCode() : bank.getRoutingNumberEncrypted()))
                .ifPresent(bank -> completed.add("bank-details"));
        // Uploads are optional, including when no document categories are configured.
        completed.add("documents");
        return List.copyOf(completed);
    }

    private List<String> requiredSections(EmployeeOnboarding onboarding) {
        try {
            return JSON.readValue(onboarding.getRequiredSections(), new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid stored onboarding required sections", exception);
        }
    }
}
