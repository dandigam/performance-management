package com.rit.performance.service;

import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeAssignment;
import com.rit.performance.entity.EmployeeRole;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.User;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.EmployeeAssignmentRepository;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.EmployeeRoleRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AssessmentAssigneeResolver {
    private final EmployeeAssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final LookupValueRepository lookupValueRepository;
    private final UserRepository userRepository;

    public Employee resolve(Employee subject, LookupValue role) {
        return resolve(subject, role, false);
    }

    public Optional<Employee> resolveIfAvailable(Employee subject, LookupValue role) {
        try {
            return Optional.of(resolve(subject, role));
        } catch (InvalidOperationException | IllegalStateException exception) {
            return Optional.empty();
        }
    }

    public Employee resolve(Employee subject, LookupValue role, boolean teamLeadPrecedes) {
        String roleName = role.getName().trim();
        String normalized = normalize(role);
        if (normalized.equals("EMPLOYEE") || normalized.equals("SELF") || normalized.equals("SELFASSESSOR"))
            return subject;
        if (normalized.equals("TEAMLEAD") || normalized.equals("LEAD")) return leadOf(subject.getId(), roleName);
        if (normalized.equals("MANAGER")) return managerOf(subject.getId(), roleName);
        if (normalized.equals("MANAGERSMANAGER")) return managerOf(managerOf(subject.getId(), roleName).getId(), roleName);
        return userRepository.findByRoleNameIgnoreCaseAndStatusIgnoreCaseOrderByIdAsc(roleName, "ACTIVE").stream()
                .map(User::getEmployee).filter(java.util.Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No active employee user can be resolved for assessor role " + roleName));
    }

    public boolean isTeamLead(LookupValue role) {
        return "TEAMLEAD".equals(normalize(role));
    }

    public boolean isPublishOnly(LookupValue role) {
        return "HR".equals(normalize(role));
    }

    public boolean isApplicable(Employee subject, LookupValue assessorRole) {
        String assessor = normalize(assessorRole);
        if (assessor.equals("EMPLOYEE") || assessor.equals("SELF") || assessor.equals("SELFASSESSOR")) return true;
        EmployeeRole currentRole = employeeRoleRepository
                .findFirstByEmployeeIdAndIsCurrentTrueOrderByEffectiveFromDesc(subject.getId())
                .orElse(null);
        LookupValue subjectRole = currentRole == null ? null
                : lookupValueRepository.findById(currentRole.getRoleId()).orElse(null);
        if (subjectRole == null) return true;
        String subjectRoleCode = normalize(subjectRole);
        if (assessor.equals("TEAMLEAD") || assessor.equals("LEAD")) return subjectRoleCode.equals("EMPLOYEE");
        if (assessor.equals("MANAGER"))
            return subjectRoleCode.equals("EMPLOYEE") || subjectRoleCode.equals("TEAMLEAD")
                    || subjectRoleCode.equals("LEAD");
        return true;
    }

    private String normalize(LookupValue role) {
        String value = role.getCode() == null || role.getCode().isBlank() ? role.getName() : role.getCode();
        return value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    private Employee managerOf(Long employeeId, String roleName) {
        Long managerId = assignmentRepository.findActiveByEmployeeId(employeeId)
                .map(EmployeeAssignment::getManagerId).orElseThrow(() -> new InvalidOperationException(
                        "No current manager found for employee " + employeeId
                                + " while resolving assessor role " + roleName));
        return employeeRepository.findById(managerId).orElseThrow(() ->
                new InvalidOperationException("Manager employee not found: " + managerId));
    }

    private Employee leadOf(Long employeeId, String roleName) {
        Long leadId = assignmentRepository.findActiveByEmployeeId(employeeId)
                .map(EmployeeAssignment::getLeadId).orElseThrow(() -> new InvalidOperationException(
                        "No current team lead found for employee " + employeeId
                                + " while resolving assessor role " + roleName));
        return employeeRepository.findById(leadId).orElseThrow(() ->
                new InvalidOperationException("Team Lead employee not found: " + leadId));
    }
}
