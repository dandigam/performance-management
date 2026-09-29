package com.rit.performance.service;

import com.rit.performance.dto.report.EmployeeWorkforceReportResponse;
import com.rit.performance.dto.report.EmployeeWorkforceReportRow;
import com.rit.performance.dto.report.EmployeeWorkforceReportSummary;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeAssignment;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.Sow;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.EmployeeAssignmentRepository;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.SowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeWorkforceReportService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final SowRepository sowRepository;
    private final LookupValueRepository lookupValueRepository;

    @Transactional(readOnly = true)
    public EmployeeWorkforceReportResponse getReport(
            String search,
            Long departmentId,
            Long designationId,
            String employmentType,
            String workMode,
            String workLocation,
            String status,
            int page,
            int size,
            String sort
    ) {
        validatePage(page, size);
        validateLookupId(departmentId, "DEPARTMENT", "departmentId");
        validateLookupId(designationId, "DESIGNATION", "designationId");
        employmentType = normalizeLookupFilter(employmentType, "EMPLOYMENT_TYPE", "employmentType");
        workMode = normalizeLookupFilter(workMode, "WORK_MODE", "workMode");
        workLocation = normalizeLookupFilter(workLocation, "WORK_LOCATION", "workLocation");
        status = normalizeLookupFilter(status, "EMPLOYEE_STATUS", "status");

        LocalDate today = LocalDate.now();
        String searchPattern = searchPattern(search);
        var employees = employeeRepository.findWorkforceReport(
                searchPattern, departmentId, designationId, employmentType, workMode,
                workLocation, status, today, PageRequest.of(page, size, reportSort(sort)));

        List<EmployeeWorkforceReportRow> content = mapRows(employees.getContent(), today);
        return new EmployeeWorkforceReportResponse(
                new EmployeeWorkforceReportSummary(employees.getTotalElements()),
                content,
                employees.getNumber(),
                employees.getSize(),
                employees.getTotalElements(),
                employees.getTotalPages());
    }

    private List<EmployeeWorkforceReportRow> mapRows(List<Employee> employees, LocalDate today) {
        if (employees.isEmpty()) return List.of();

        Set<Long> employeeIds = employees.stream().map(Employee::getId).collect(Collectors.toSet());
        List<EmployeeAssignment> assignments = assignmentRepository.findCurrentForEmployees(employeeIds.stream().toList(), today);
        Map<Long, List<EmployeeAssignment>> assignmentsByEmployee = assignments.stream()
                .collect(Collectors.groupingBy(EmployeeAssignment::getEmployeeId));
        Map<Long, Sow> sows = sowRepository.findAllById(assignments.stream()
                        .map(EmployeeAssignment::getSowId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Sow::getId, Function.identity()));
        Map<Long, LookupValue> designations = lookupValueRepository.findAllById(employees.stream()
                        .map(Employee::getDesignationId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(LookupValue::getId, Function.identity()));

        return employees.stream().map(employee -> {
            Map<Long, LookupValue> departments = new LinkedHashMap<>();
            for (EmployeeAssignment assignment : assignmentsByEmployee.getOrDefault(employee.getId(), List.of())) {
                Sow sow = sows.get(assignment.getSowId());
                if (sow != null && sow.getBusinessUnit() != null) {
                    departments.putIfAbsent(sow.getBusinessUnit().getId(), sow.getBusinessUnit());
                }
            }
            LookupValue department = departments.size() == 1
                    ? departments.values().iterator().next() : null;
            LookupValue designation = designations.get(employee.getDesignationId());
            String employeeName = java.util.stream.Stream.of(employee.getFirstName(), employee.getLastName())
                    .filter(Objects::nonNull).map(String::trim).filter(value -> !value.isEmpty())
                    .collect(Collectors.joining(" "));
            return new EmployeeWorkforceReportRow(
                    employee.getId(), employee.getRitId(), employeeName, employee.getEmail(),
                    department == null ? null : department.getName(),
                    designation == null ? null : designation.getName(),
                    employee.getEmploymentType(), employee.getWorkMode(), employee.getWorkLocation(),
                    employee.getStatus(), employee.getJoiningDate());
        }).toList();
    }

    private void validateLookupId(Long id, String lookupType, String field) {
        if (id == null) return;
        if (id < 1) throw new InvalidOperationException(field + " must be positive");
        LookupValue value = lookupValueRepository.findById(id)
                .orElseThrow(() -> new InvalidOperationException(field + " must reference an active " + lookupType + " lookup value"));
        if (!value.isActive() || !value.getLookupType().isActive()
                || !lookupType.equalsIgnoreCase(value.getLookupType().getCode())) {
            throw new InvalidOperationException(field + " must reference an active " + lookupType + " lookup value");
        }
    }

    private String normalizeLookupFilter(String value, String lookupType, String field) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return lookupValueRepository
                .findByLookupTypeCodeIgnoreCaseAndCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrue(
                        lookupType, normalized)
                .map(LookupValue::getCode)
                .orElseThrow(() -> new InvalidOperationException(
                        field + " must be an active " + lookupType + " lookup value"));
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidOperationException("page must be at least 0 and size must be between 1 and 100");
        }
    }

    private static String searchPattern(String search) {
        if (search == null || search.isBlank()) return null;
        return "%" + search.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private static Sort reportSort(String value) {
        String[] parts = (value == null || value.isBlank() ? "employeeName,asc" : value).split(",", -1);
        if (parts.length > 2 || (parts.length == 2 && !Set.of("asc", "desc")
                .contains(parts[1].trim().toLowerCase(Locale.ROOT)))) {
            throw new InvalidOperationException("sort must use field,asc or field,desc");
        }
        String field = parts[0].trim();
        Sort.Direction direction = parts.length == 2
                ? Sort.Direction.fromString(parts[1].trim()) : Sort.Direction.ASC;
        if ("employeeName".equals(field)) {
            return Sort.by(direction, "firstName", "lastName").and(Sort.by("id"));
        }
        String entityField = switch (field) {
            case "employeeId" -> "id";
            case "employeeNumber" -> "ritId";
            case "email", "employmentType", "workMode", "workLocation", "status", "joiningDate" -> field;
            default -> throw new InvalidOperationException("Unsupported sort field: " + field);
        };
        return Sort.by(direction, entityField).and(Sort.by("id"));
    }
}
