package com.rit.performance.service;

import com.rit.performance.entity.Employee;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.EmployeeOrgHierarchyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AssessmentAssigneeResolver {
    private final EmployeeOrgHierarchyRepository hierarchyRepository;

    public static String stageCode(LookupValue role) {
        String value = role.getCode() == null || role.getCode().isBlank() ? role.getName() : role.getCode();
        return switch (value.replaceAll("[^A-Za-z0-9]", "").toUpperCase()) {
            case "SELF" -> "SELF";
            case "REPORTINGMANAGER1" -> "REPORTING_MANAGER_1";
            case "REPORTINGMANAGER2" -> "REPORTING_MANAGER_2";
            case "HR" -> "HR";
            default -> throw new InvalidOperationException("Unsupported performance stage: " + value);
        };
    }

    public boolean isPublishOnly(LookupValue role) {
        return "HR".equals(stageCode(role));
    }

    public boolean isConfigured(Employee employee) {
        return hierarchyRepository.findByEmployee_Id(employee.getId()).isPresent();
    }

    public Optional<Employee> resolveIfAvailable(Employee subject, LookupValue role) {
        return switch (stageCode(role)) {
            case "SELF" -> Optional.of(subject);
            case "REPORTING_MANAGER_1" -> reportingManager(subject, 1);
            case "REPORTING_MANAGER_2" -> reportingManager(subject, 2);
            default -> Optional.empty();
        };
    }

    private Optional<Employee> reportingManager(Employee subject, int level) {
        var chain = reportingChain(subject);
        return chain.size() < level ? Optional.empty() : Optional.of(chain.get(level - 1));
    }

    public java.util.List<Employee> reportingChain(Employee subject) {
        Employee current = subject;
        java.util.List<Employee> chain = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        visited.add(subject.getId());
        // Only the subject and Manager 1 rows are needed to identify two assessors.
        while (current != null && chain.size() < 2) {
            Long currentId = current.getId();
            var row = hierarchyRepository.findByEmployee_Id(currentId).orElse(null);
            if (row == null) {
                // An assigned manager may be the top of the organization without their own row.
                if (!chain.isEmpty()) break;
                throw new InvalidOperationException("Missing organization hierarchy for employee " + currentId);
            }
            current = row.getReportingManager();
            if (current != null) {
                if (!visited.add(current.getId()))
                    throw new InvalidOperationException("Reporting cycle for employee " + subject.getId());
                if (!"ACTIVE".equalsIgnoreCase(current.getStatus()))
                    throw new InvalidOperationException("Inactive reporting manager: " + current.getId());
                chain.add(current);
            }
        }
        return chain;
    }
}
