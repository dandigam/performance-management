package com.rit.performance.service;

import com.rit.performance.dto.EmployeeSummaryResponse;
import com.rit.performance.dto.EmployeeSummaryPageResponse;
import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import com.rit.performance.exception.InvalidOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeSummaryService {
    private final EmployeeRepository employeeRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final SowRepository sowRepository;
    private final LookupValueRepository lookupValueRepository;

    @Transactional(readOnly = true)
    public EmployeeSummaryPageResponse getSummaries(int page, int size) {
        return getSummaries(page, size, null, null, null, null, null, null, "employeeName,asc");
    }

    @Transactional(readOnly = true)
    public EmployeeSummaryPageResponse getSummaries(int page, int size, String search,
            Long departmentId, Long sowId, String assignmentStatus, String workMode,
            String status, String sort) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidOperationException("page must be at least 0 and size must be between 1 and 100");
        }
        assignmentStatus = normalizeFilter(assignmentStatus, Set.of("ASSIGNED", "UNASSIGNED"), "assignmentStatus");
        workMode = normalizeFilter(workMode, Set.of("ONSITE", "OFFSHORE"), "workMode");
        status = normalizeFilter(status, Set.of("ACTIVE", "INACTIVE"), "status");
        if ((departmentId != null && departmentId < 1) || (sowId != null && sowId < 1)) {
            throw new InvalidOperationException("departmentId and sowId must be positive");
        }
        String pattern = search == null || search.isBlank() ? null : "%" + search.trim()
                .toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        LocalDate today = LocalDate.now();
        var employees = employeeRepository.findSummaries(pattern, departmentId, sowId,
                assignmentStatus, workMode, status, today, PageRequest.of(page, size, summarySort(sort)));
        if (employees.isEmpty()) {
            return new EmployeeSummaryPageResponse(List.of(), page, size, employees.getTotalElements(),
                    employees.getTotalPages(), employees.isFirst(), employees.isLast());
        }
        var assignments = assignmentRepository.findCurrentForEmployees(
                employees.getContent().stream().map(Employee::getId).toList(), today);
        var byEmployee = assignments.stream().collect(Collectors.groupingBy(EmployeeAssignment::getEmployeeId));
        var sows = sowRepository.findAllById(assignments.stream().map(EmployeeAssignment::getSowId)
                .filter(Objects::nonNull).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Sow::getId, Function.identity()));
        var designations = lookupValueRepository.findAllById(employees.getContent().stream()
                .map(Employee::getDesignationId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(LookupValue::getId, Function.identity()));
        var content = employees.getContent().stream().map(employee -> {
            var projects = new LinkedHashMap<Long, Sow>();
            for (var assignment : byEmployee.getOrDefault(employee.getId(), List.of())) {
                var sow = sows.get(assignment.getSowId());
                if (sow != null) projects.putIfAbsent(sow.getId(), sow);
            }
            // A single department is meaningful only when current projects agree.
            var departments = projects.values().stream().map(Sow::getBusinessUnit)
                    .filter(Objects::nonNull).collect(Collectors.toMap(LookupValue::getId,
                            Function.identity(), (first, second) -> first));
            var department = departments.size() == 1 ? departments.values().iterator().next() : null;
            var designation = designations.get(employee.getDesignationId());
            var name = java.util.stream.Stream.of(employee.getFirstName(), employee.getLastName())
                    .filter(Objects::nonNull).map(String::trim).filter(value -> !value.isEmpty())
                    .collect(Collectors.joining(" "));
            return new EmployeeSummaryResponse(employee.getId(), name, employee.getRitId(), employee.getEmail(),
                    employee.getDesignationId(), designation == null ? null : designation.getName(),
                    employee.getEmploymentType(), employee.getWorkMode(), employee.getWorkLocation(),
                    employee.getStatus(), department == null ? null : department.getId(),
                    department == null ? null : department.getName(), projects.values().stream()
                            .map(sow -> new EmployeeSummaryResponse.ProjectSummary(sow.getId(), sow.getSowName())).toList());
        }).toList();
        return new EmployeeSummaryPageResponse(content, page, size, employees.getTotalElements(),
                employees.getTotalPages(), employees.isFirst(), employees.isLast());
    }

    private static String normalizeFilter(String value, Set<String> allowed, String field) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) throw new InvalidOperationException("Invalid " + field);
        return normalized;
    }

    private static Sort summarySort(String value) {
        String[] parts = (value == null || value.isBlank() ? "employeeName,asc" : value).split(",", -1);
        if (parts.length > 2 || (parts.length == 2 && !Set.of("asc", "desc")
                .contains(parts[1].trim().toLowerCase(Locale.ROOT)))) {
            throw new InvalidOperationException("sort must use field,asc or field,desc");
        }
        String field = parts[0].trim();
        Sort.Direction direction = parts.length == 2 ? Sort.Direction.fromString(parts[1].trim()) : Sort.Direction.ASC;
        if ("employeeName".equals(field)) return Sort.by(direction, "firstName", "lastName").and(Sort.by("id"));
        if ("employeeId".equals(field)) return Sort.by(direction, "id");
        if (!Set.of("ritId", "email", "employmentType", "workMode", "workLocation", "status").contains(field)) {
            throw new InvalidOperationException("Unsupported sort field: " + field);
        }
        return Sort.by(direction, field).and(Sort.by("id"));
    }
}
