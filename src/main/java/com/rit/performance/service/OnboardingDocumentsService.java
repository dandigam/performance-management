package com.rit.performance.service;
import com.rit.performance.dto.OnboardingResponse;
import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import com.rit.performance.security.AuthenticatedUser;
import com.rit.performance.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
@RequiredArgsConstructor
public class OnboardingDocumentsService {
    private final CurrentEmployeeService currentEmployee;
    private final EmployeeOnboardingRepository onboardings;
    private final DocumentRepository documents;
    private final LookupValueRepository lookups;
    private final EmployeeRepository employees;
    private final OnboardingSelfService self;
    @Transactional
    public OnboardingResponse update(List<Long> ids) {
        var employee = currentEmployee.currentEmployee();
        var onboarding = onboardings.findForUpdateByEmployeeId(employee.getId()).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, "ONBOARDING_NOT_FOUND", "Onboarding record not found."));
        if (!Set.of("INVITED", "IN_PROGRESS", "CHANGES_REQUESTED").contains(onboarding.getStatus()))
            throw new ApplicationException(HttpStatus.CONFLICT, "ONBOARDING_NOT_EDITABLE", "Documents cannot be changed at this stage.");
        var principal = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var types = new HashMap<String, LookupValue>();
        String mode = employee.getWorkMode();
        if (!"ONSITE".equals(mode) && !"OFFSHORE".equals(mode))
            throw new ApplicationException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_WORK_MODE", "A work mode is required.");
        lookups.findByLookupTypeCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrueOrderByDisplayOrderAscIdAsc(
                "EMPLOYEE_ONBOARDING_DOCUMENTS_" + mode).forEach(type -> types.put(type.getCode(), type));
        var requested = new LinkedHashSet<>(ids);
        var selected = documents.findAllById(requested);
        if (selected.size() != requested.size()) throw invalidDocument();
        // Validate the entire request before changing existing associations.
        for (var document : selected) {
            boolean alreadyOwned = employee.getEmployeeDocuments().stream()
                    .anyMatch(link -> link.getDocument().getId().equals(document.getId()));
            if ((!alreadyOwned && !Objects.equals(document.getCreatedBy(), principal.id()))
                    || !"EMPLOYEE".equals(document.getModule()) || !types.containsKey(document.getDocumentType()))
                throw invalidDocument();
        }
        employee.getEmployeeDocuments().removeIf(link ->
                link.getDocumentType() != null && types.containsKey(link.getDocumentType().getCode())
                && !requested.contains(link.getDocument().getId()));
        for (var document : selected) {
            var existing = employee.getEmployeeDocuments().stream()
                    .filter(link -> link.getDocument().getId().equals(document.getId())).findFirst();
            if (existing.isPresent()) existing.get().setStatus("ACTIVE");
            else employee.getEmployeeDocuments().add(EmployeeDocument.builder()
                    .id(new EmployeeDocumentId(employee.getId(), document.getId()))
                    .employee(employee).document(document).documentType(types.get(document.getDocumentType()))
                    .status("ACTIVE").build());
        }
        employees.saveAndFlush(employee);
        if ("INVITED".equals(onboarding.getStatus())) onboarding.setStatus("IN_PROGRESS");
        onboarding.setUpdatedOn(java.time.LocalDateTime.now());
        onboardings.saveAndFlush(onboarding);
        return self.getMine();
    }
    private ApplicationException invalidDocument() {
        return new ApplicationException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_DOCUMENT",
                "Select your own uploaded employee documents with an active onboarding category.");
    }
}
