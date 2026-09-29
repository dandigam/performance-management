package com.rit.performance.service;

import tools.jackson.databind.JsonNode;
import com.rit.performance.dto.report.*;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeAssignment;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.Sow;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.EmployeeAssignmentRepository;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.SowRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeReportQueryService {

    private static final String REPORT_CODE = "EMPLOYEE_WORKFORCE";
    private static final List<ReportOption> ASSIGNMENT_OPTIONS = List.of(
            new ReportOption("ASSIGNED", "Assigned"),
            new ReportOption("UNASSIGNED", "Unassigned"));
    private static final List<String> TEXT_OPERATORS = List.of("CONTAINS", "EQUALS", "IS_EMPTY");
    private static final List<String> LOOKUP_OPERATORS = List.of("EQUALS", "IN", "IS_EMPTY");
    private static final List<String> DATE_OPERATORS = List.of("EQUALS", "BEFORE", "AFTER", "IS_EMPTY");

    private static final LinkedHashMap<String, FieldDefinition> FIELDS = fields();

    private final EmployeeRepository employeeRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final SowRepository sowRepository;
    private final LookupValueRepository lookupValueRepository;

    public ReportDefinitionResponse definition() {
        return new ReportDefinitionResponse(REPORT_CODE,
                FIELDS.values().stream().map(FieldDefinition::apiDefinition).toList());
    }

    @Transactional(readOnly = true)
    public EmployeeReportExportData exportData(ReportExportRequest request) {
        List<String> selectedKeys = selectedColumns(request.columns());
        List<ReportFilterRequest> filters = request.filters() == null ? List.of() : request.filters();
        List<ReportSortRequest> sorts = request.sort() == null || request.sort().isEmpty()
                ? List.of(new ReportSortRequest("employeeName", "ASC")) : request.sort();
        validateFilters(filters);

        LocalDate today = LocalDate.now();
        List<Employee> employees = employeeRepository.findAll(
                specification(filters, today), buildSort(sorts));
        List<Map<String, Object>> content = rows(employees, selectedKeys, today);
        applyDisplayLabels(content, selectedKeys);
        List<ReportResultColumn> columns = selectedKeys.stream()
                .map(FIELDS::get)
                .map(field -> new ReportResultColumn(field.key(), field.label(), field.type()))
                .toList();
        return new EmployeeReportExportData(columns, content);
    }

    @Transactional(readOnly = true)
    public GenericReportResponse query(ReportQueryRequest request) {
        List<String> selectedKeys = selectedColumns(request.columns());
        List<ReportFilterRequest> filters = request.filters() == null ? List.of() : request.filters();
        List<ReportSortRequest> sorts = request.sort() == null || request.sort().isEmpty()
                ? List.of(new ReportSortRequest("employeeName", "ASC")) : request.sort();
        int pageNumber = request.page() == null ? 0 : request.page();
        int pageSize = request.size() == null ? 25 : request.size();
        validatePage(pageNumber, pageSize);
        validateFilters(filters);

        LocalDate today = LocalDate.now();
        Specification<Employee> contentSpec = specification(filters, today);
        Sort sort = buildSort(sorts);
        var page = employeeRepository.findAll(contentSpec, PageRequest.of(pageNumber, pageSize, sort));

        Specification<Employee> summarySpec = specification(filters, today);
        long totalEmployees = employeeRepository.count(summarySpec);
        long assignedEmployees = employeeRepository.count(summarySpec.and(assignmentSpecification(today, true)));

        List<Map<String, Object>> content = rows(page.getContent(), selectedKeys, today);
        List<ReportResultColumn> columns = selectedKeys.stream()
                .map(FIELDS::get)
                .map(field -> new ReportResultColumn(field.key(), field.label(), field.type()))
                .toList();
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("totalEmployees", totalEmployees);
        summary.put("assignedEmployees", assignedEmployees);
        summary.put("unassignedEmployees", totalEmployees - assignedEmployees);

        return new GenericReportResponse(summary, columns, content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private Specification<Employee> specification(List<ReportFilterRequest> filters, LocalDate today) {
        Specification<Employee> result = (root, query, cb) -> cb.conjunction();
        for (ReportFilterRequest filter : filters) {
            result = result.and(filterSpecification(filter, today));
        }
        return result;
    }

    private Specification<Employee> filterSpecification(ReportFilterRequest filter, LocalDate today) {
        String field = filter.field();
        String operator = filter.operator().toUpperCase(Locale.ROOT);
        return switch (field) {
            case "employeeName" -> stringSpecification(null, operator, filter.value(), true);
            case "employeeNumber" -> stringSpecification("ritId", operator, filter.value(), false);
            case "email" -> stringSpecification("email", operator, filter.value(), false);
            case "employmentType" -> stringSpecification("employmentType", operator, filter.value(), false);
            case "workMode" -> stringSpecification("workMode", operator, filter.value(), false);
            case "workLocation" -> stringSpecification("workLocation", operator, filter.value(), false);
            case "status" -> stringSpecification("status", operator, filter.value(), false);
            case "designationId" -> idSpecification("designationId", operator, filter.value());
            case "designationName" -> designationNameSpecification(operator, filter.value());
            case "departmentId" -> departmentSpecification(operator, filter.value(), today);
            case "assignmentStatus" -> assignmentStatusSpecification(operator, filter.value(), today);
            case "joiningDate" -> dateSpecification("joiningDate", operator, filter.value());
            default -> throw invalidField(field);
        };
    }

    private Specification<Employee> stringSpecification(
            String attribute, String operator, JsonNode value, boolean employeeName) {
        return (root, query, cb) -> {
            Expression<String> expression = employeeName
                    ? cb.concat(cb.concat(cb.coalesce(root.get("firstName"), ""), " "),
                            cb.coalesce(root.get("lastName"), ""))
                    : root.get(attribute);
            if ("IS_EMPTY".equals(operator)) {
                return cb.or(cb.isNull(expression), cb.equal(cb.trim(expression), ""));
            }
            if ("IN".equals(operator)) {
                List<String> expected = textValues(value, true).stream()
                        .map(item -> item.toLowerCase(Locale.ROOT)).toList();
                return cb.lower(expression).in(expected);
            }
            String expected = requiredText(value).toLowerCase(Locale.ROOT);
            Expression<String> lowered = cb.lower(expression);
            return "CONTAINS".equals(operator)
                    ? cb.like(lowered, "%" + escapeLike(expected) + "%", '!')
                    : cb.equal(lowered, expected);
        };
    }

    private Specification<Employee> idSpecification(String attribute, String operator, JsonNode value) {
        return (root, query, cb) -> {
            if ("IS_EMPTY".equals(operator)) return cb.isNull(root.get(attribute));
            List<Long> values = longValues(value, "IN".equals(operator));
            return "IN".equals(operator) ? root.get(attribute).in(values)
                    : cb.equal(root.get(attribute), values.get(0));
        };
    }

    private Specification<Employee> designationNameSpecification(String operator, JsonNode value) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<LookupValue> designation = subquery.from(LookupValue.class);
            subquery.select(designation.get("id"));
            if ("IS_EMPTY".equals(operator)) return cb.isNull(root.get("designationId"));
            String expected = requiredText(value).toLowerCase(Locale.ROOT);
            Predicate name = "CONTAINS".equals(operator)
                    ? cb.like(cb.lower(designation.get("name")), "%" + escapeLike(expected) + "%", '!')
                    : cb.equal(cb.lower(designation.get("name")), expected);
            subquery.where(cb.equal(designation.get("id"), root.get("designationId")), name);
            return cb.exists(subquery);
        };
    }

    private Specification<Employee> departmentSpecification(String operator, JsonNode value, LocalDate today) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<EmployeeAssignment> assignment = subquery.from(EmployeeAssignment.class);
            Root<Sow> sow = subquery.from(Sow.class);
            subquery.select(assignment.get("id"));
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(assignment.get("employeeId"), root.get("id")));
            predicates.add(cb.equal(cb.upper(assignment.get("status")), "ASSIGNED"));
            predicates.add(cb.lessThanOrEqualTo(assignment.get("effectiveFrom"), today));
            predicates.add(cb.or(cb.isNull(assignment.get("effectiveTo")),
                    cb.greaterThanOrEqualTo(assignment.get("effectiveTo"), today)));
            predicates.add(cb.equal(assignment.get("sowId"), sow.get("id")));
            if (!"IS_EMPTY".equals(operator)) {
                List<Long> ids = longValues(value, "IN".equals(operator));
                predicates.add("IN".equals(operator)
                        ? sow.get("businessUnit").get("id").in(ids)
                        : cb.equal(sow.get("businessUnit").get("id"), ids.get(0)));
            } else {
                predicates.add(cb.isNotNull(sow.get("businessUnit")));
            }
            subquery.where(predicates.toArray(Predicate[]::new));
            return "IS_EMPTY".equals(operator) ? cb.not(cb.exists(subquery)) : cb.exists(subquery);
        };
    }

    private Specification<Employee> assignmentStatusSpecification(String operator, JsonNode value, LocalDate today) {
        String status = requiredText(value).toUpperCase(Locale.ROOT);
        boolean assigned = "ASSIGNED".equals(status);
        return assignmentSpecification(today, assigned);
    }

    private Specification<Employee> assignmentSpecification(LocalDate today, boolean assigned) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = currentAssignmentSubquery(root, query.subquery(Long.class), cb, today);
            return assigned ? cb.exists(subquery) : cb.not(cb.exists(subquery));
        };
    }

    private Subquery<Long> currentAssignmentSubquery(
            Root<Employee> employee, Subquery<Long> subquery,
            jakarta.persistence.criteria.CriteriaBuilder cb, LocalDate today) {
        Root<EmployeeAssignment> assignment = subquery.from(EmployeeAssignment.class);
        subquery.select(assignment.get("id"));
        subquery.where(
                cb.equal(assignment.get("employeeId"), employee.get("id")),
                cb.equal(cb.upper(assignment.get("status")), "ASSIGNED"),
                cb.lessThanOrEqualTo(assignment.get("effectiveFrom"), today),
                cb.or(cb.isNull(assignment.get("effectiveTo")),
                        cb.greaterThanOrEqualTo(assignment.get("effectiveTo"), today)));
        return subquery;
    }

    private Specification<Employee> dateSpecification(String attribute, String operator, JsonNode value) {
        return (root, query, cb) -> {
            if ("IS_EMPTY".equals(operator)) return cb.isNull(root.get(attribute));
            LocalDate date;
            try {
                date = LocalDate.parse(requiredText(value));
            } catch (RuntimeException ex) {
                throw new InvalidOperationException(attribute + " requires an ISO date (yyyy-MM-dd)");
            }
            return switch (operator) {
                case "BEFORE" -> cb.lessThan(root.get(attribute), date);
                case "AFTER" -> cb.greaterThan(root.get(attribute), date);
                default -> cb.equal(root.get(attribute), date);
            };
        };
    }

    private List<Map<String, Object>> rows(List<Employee> employees, List<String> keys, LocalDate today) {
        if (employees.isEmpty()) return List.of();
        List<Long> employeeIds = employees.stream().map(Employee::getId).toList();
        List<EmployeeAssignment> assignments = assignmentRepository.findCurrentForEmployees(employeeIds, today);
        Map<Long, List<EmployeeAssignment>> byEmployee = assignments.stream()
                .collect(Collectors.groupingBy(EmployeeAssignment::getEmployeeId));
        Map<Long, Sow> sows = sowRepository.findAllById(assignments.stream().map(EmployeeAssignment::getSowId)
                        .filter(Objects::nonNull).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Sow::getId, Function.identity()));
        Map<Long, LookupValue> designations = lookupValueRepository.findAllById(employees.stream()
                        .map(Employee::getDesignationId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(LookupValue::getId, Function.identity()));

        return employees.stream().map(employee -> {
            List<EmployeeAssignment> current = byEmployee.getOrDefault(employee.getId(), List.of());
            Map<Long, LookupValue> departments = new LinkedHashMap<>();
            for (EmployeeAssignment assignment : current) {
                Sow sow = sows.get(assignment.getSowId());
                if (sow != null && sow.getBusinessUnit() != null) {
                    departments.putIfAbsent(sow.getBusinessUnit().getId(), sow.getBusinessUnit());
                }
            }
            LookupValue department = departments.size() == 1 ? departments.values().iterator().next() : null;
            LookupValue designation = designations.get(employee.getDesignationId());
            Map<String, Object> values = new LinkedHashMap<>();
            Map<String, Object> all = new LinkedHashMap<>();
            all.put("employeeId", employee.getId());
            all.put("employeeNumber", employee.getRitId());
            all.put("employeeName", employeeName(employee));
            all.put("email", employee.getEmail());
            all.put("departmentId", department == null ? null : department.getId());
            all.put("departmentName", department == null ? null : department.getName());
            all.put("designationId", employee.getDesignationId());
            all.put("designationName", designation == null ? null : designation.getName());
            all.put("employmentType", employee.getEmploymentType());
            all.put("workMode", employee.getWorkMode());
            all.put("workLocation", employee.getWorkLocation());
            all.put("status", employee.getStatus());
            all.put("assignmentStatus", current.isEmpty() ? "UNASSIGNED" : "ASSIGNED");
            all.put("joiningDate", employee.getJoiningDate());
            keys.forEach(key -> values.put(key, all.get(key)));
            return values;
        }).toList();
    }

    private void applyDisplayLabels(List<Map<String, Object>> rows, List<String> keys) {
        for (String key : keys) {
            FieldDefinition field = FIELDS.get(key);
            if (field.lookupCode() != null) {
                Map<Object, String> labels = lookupValueRepository
                        .findByLookupTypeCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrueOrderByDisplayOrderAscIdAsc(
                                field.lookupCode())
                        .stream().collect(Collectors.toMap(
                                value -> key.endsWith("Id") ? value.getId() : value.getCode().toUpperCase(Locale.ROOT),
                                LookupValue::getName,
                                (first, second) -> first));
                for (Map<String, Object> row : rows) {
                    Object value = row.get(key);
                    Object lookupKey = value instanceof String text ? text.toUpperCase(Locale.ROOT) : value;
                    if (value != null && labels.containsKey(lookupKey)) row.put(key, labels.get(lookupKey));
                }
            } else if (field.options() != null) {
                Map<String, String> labels = field.options().stream().collect(Collectors.toMap(
                        option -> option.value().toUpperCase(Locale.ROOT), ReportOption::label));
                for (Map<String, Object> row : rows) {
                    Object value = row.get(key);
                    if (value instanceof String text && labels.containsKey(text.toUpperCase(Locale.ROOT))) {
                        row.put(key, labels.get(text.toUpperCase(Locale.ROOT)));
                    }
                }
            }
        }
    }

    private void validateFilters(List<ReportFilterRequest> filters) {
        for (ReportFilterRequest filter : filters) {
            FieldDefinition field = FIELDS.get(filter.field());
            if (field == null || !field.filterable()) throw invalidField(filter.field());
            String operator = filter.operator().toUpperCase(Locale.ROOT);
            if (!field.operators().contains(operator)) {
                throw new InvalidOperationException("Operator " + operator + " is not allowed for " + filter.field());
            }
            if (Set.of("departmentId", "designationId").contains(filter.field())
                    && !"IS_EMPTY".equals(operator)) {
                longValues(filter.value(), "IN".equals(operator));
            } else if ("IN".equals(operator)) {
                textValues(filter.value(), true);
            } else if (!"IS_EMPTY".equals(operator)) {
                requiredText(filter.value());
            }
            if ("joiningDate".equals(filter.field()) && !"IS_EMPTY".equals(operator)) {
                try {
                    LocalDate.parse(requiredText(filter.value()));
                } catch (RuntimeException ex) {
                    throw new InvalidOperationException("joiningDate requires an ISO date (yyyy-MM-dd)");
                }
            }
            if ("assignmentStatus".equals(filter.field())) {
                String value = requiredText(filter.value()).toUpperCase(Locale.ROOT);
                if (!Set.of("ASSIGNED", "UNASSIGNED").contains(value)) {
                    throw new InvalidOperationException("assignmentStatus must be ASSIGNED or UNASSIGNED");
                }
            }
        }
    }

    private static List<String> selectedColumns(List<String> requested) {
        List<String> keys = requested == null || requested.isEmpty()
                ? FIELDS.values().stream().filter(FieldDefinition::defaultVisible).map(FieldDefinition::key).toList()
                : requested;
        LinkedHashSet<String> unique = new LinkedHashSet<>(keys);
        for (String key : unique) if (!FIELDS.containsKey(key)) throw invalidField(key);
        return List.copyOf(unique);
    }

    private static Sort buildSort(List<ReportSortRequest> sorts) {
        Sort result = Sort.unsorted();
        for (ReportSortRequest requested : sorts) {
            FieldDefinition field = FIELDS.get(requested.field());
            if (field == null || !field.sortable() || field.sortAttributes().isEmpty()) {
                throw new InvalidOperationException("Field is not sortable: " + requested.field());
            }
            Sort.Direction direction;
            try {
                direction = Sort.Direction.fromString(requested.direction());
            } catch (IllegalArgumentException ex) {
                throw new InvalidOperationException("Sort direction must be ASC or DESC");
            }
            for (String attribute : field.sortAttributes()) {
                result = result.and(Sort.by(direction, attribute));
            }
        }
        return result.and(Sort.by("id"));
    }

    private static LinkedHashMap<String, FieldDefinition> fields() {
        LinkedHashMap<String, FieldDefinition> fields = new LinkedHashMap<>();
        add(fields, "employeeId", "Employee ID", "NUMBER", null, false, true, false, List.of(), List.of("id"), null);
        add(fields, "employeeNumber", "Employee number", "TEXT", null, true, true, true, TEXT_OPERATORS, List.of("ritId"), null);
        add(fields, "employeeName", "Employee", "TEXT", null, true, true, true, TEXT_OPERATORS, List.of("firstName", "lastName"), null);
        add(fields, "email", "Email", "TEXT", null, true, true, true, TEXT_OPERATORS, List.of("email"), null);
        add(fields, "departmentId", "Department", "LOOKUP", "DEPARTMENT", false, false, true, LOOKUP_OPERATORS, List.of(), null);
        add(fields, "departmentName", "Department", "TEXT", null, true, false, false, List.of(), List.of(), null);
        add(fields, "designationId", "Designation", "LOOKUP", "DESIGNATION", false, false, true, LOOKUP_OPERATORS, List.of(), null);
        add(fields, "designationName", "Designation", "TEXT", null, true, false, true, TEXT_OPERATORS, List.of(), null);
        add(fields, "employmentType", "Employment type", "LOOKUP", "EMPLOYMENT_TYPE", true, true, true, LOOKUP_OPERATORS, List.of("employmentType"), null);
        add(fields, "workMode", "Work mode", "LOOKUP", "WORK_MODE", true, true, true, LOOKUP_OPERATORS, List.of("workMode"), null);
        add(fields, "workLocation", "Work location", "LOOKUP", "WORK_LOCATION", true, true, true, LOOKUP_OPERATORS, List.of("workLocation"), null);
        add(fields, "status", "Employee status", "LOOKUP", "EMPLOYEE_STATUS", true, true, true, LOOKUP_OPERATORS, List.of("status"), null);
        add(fields, "assignmentStatus", "Assignment status", "ENUM", null, false, false, true,
                List.of("EQUALS"), List.of(), ASSIGNMENT_OPTIONS);
        add(fields, "joiningDate", "Joining date", "DATE", null, true, true, true, DATE_OPERATORS, List.of("joiningDate"), null);
        return fields;
    }

    private static void add(Map<String, FieldDefinition> fields, String key, String label, String type,
            String lookupCode, boolean defaultVisible, boolean sortable, boolean filterable,
            List<String> operators, List<String> sortAttributes, List<ReportOption> options) {
        fields.put(key, new FieldDefinition(key, label, type, lookupCode, defaultVisible,
                sortable, filterable, operators, sortAttributes, options));
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidOperationException("page must be at least 0 and size must be between 1 and 100");
        }
    }

    private static String employeeName(Employee employee) {
        return java.util.stream.Stream.of(employee.getFirstName(), employee.getLastName())
                .filter(Objects::nonNull).map(String::trim).filter(value -> !value.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private static String requiredText(JsonNode value) {
        if (value == null || value.isNull() || !value.isValueNode() || value.asText().isBlank()) {
            throw new InvalidOperationException("Filter value is required");
        }
        return value.asText().trim();
    }

    private static List<Long> longValues(JsonNode value, boolean arrayRequired) {
        if (arrayRequired) {
            if (value == null || !value.isArray() || value.isEmpty()) {
                throw new InvalidOperationException("IN requires a non-empty array value");
            }
            List<Long> result = new ArrayList<>();
            value.forEach(item -> result.add(positiveLookupId(item)));
            return result;
        }
        return List.of(positiveLookupId(value));
    }

    private static long positiveLookupId(JsonNode value) {
        long id;
        try {
            if (value != null && value.isIntegralNumber()) {
                id = value.longValue();
            } else if (value != null && value.isTextual()) {
                id = Long.parseLong(value.asText().trim());
            } else {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            throw new InvalidOperationException("Filter value must be a positive numeric lookup ID");
        }
        if (id < 1) {
            throw new InvalidOperationException("Filter value must be a positive numeric lookup ID");
        }
        return id;
    }

    private static List<String> textValues(JsonNode value, boolean arrayRequired) {
        if (!arrayRequired) return List.of(requiredText(value));
        if (value == null || !value.isArray() || value.isEmpty()) {
            throw new InvalidOperationException("IN requires a non-empty array value");
        }
        List<String> result = new ArrayList<>();
        value.forEach(item -> result.add(requiredText(item)));
        return result;
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private static InvalidOperationException invalidField(String field) {
        return new InvalidOperationException("Unsupported report field: " + field);
    }

    private record FieldDefinition(
            String key, String label, String type, String lookupCode,
            boolean defaultVisible, boolean sortable, boolean filterable,
            List<String> operators, List<String> sortAttributes, List<ReportOption> options) {
        ReportColumnDefinition apiDefinition() {
            return new ReportColumnDefinition(key, label, type, lookupCode, true,
                    defaultVisible, sortable, filterable, operators, options);
        }
    }
}
