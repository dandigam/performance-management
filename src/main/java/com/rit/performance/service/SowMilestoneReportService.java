package com.rit.performance.service;

import com.rit.performance.dto.report.*;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.SowMilestone;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.SowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class SowMilestoneReportService {

    private static final String REPORT_CODE = "SOW_MILESTONES";
    private static final List<String> TEXT = List.of("CONTAINS", "EQUALS", "IS_EMPTY");
    private static final List<String> LOOKUP = List.of("EQUALS", "IN", "IS_EMPTY");
    private static final List<String> DATE = List.of("EQUALS", "BEFORE", "AFTER", "IS_EMPTY");
    private static final List<String> BOOLEAN = List.of("EQUALS");
    private static final List<ReportOption> MILESTONE_STATUSES = List.of(
            new ReportOption("PLANNING", "Planning"),
            new ReportOption("NOT_STARTED", "Not started"),
            new ReportOption("IN_PROGRESS", "In progress"),
            new ReportOption("COMPLETED", "Completed"),
            new ReportOption("ON_HOLD", "On hold"),
            new ReportOption("CANCELLED", "Cancelled"));
    private static final LinkedHashMap<String, Field> FIELDS = fields();

    private final SowRepository sowRepository;

    public ReportDefinitionResponse definition() {
        return new ReportDefinitionResponse(REPORT_CODE,
                FIELDS.values().stream().map(Field::definition).toList());
    }

    @Transactional(readOnly = true)
    public GenericReportResponse query(ReportQueryRequest request) {
        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 25 : request.size();
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidOperationException("page must be at least 0 and size must be between 1 and 100");
        }
        Prepared prepared = prepare(request.columns(), request.filters(), request.sort());
        int from = Math.min(page * size, prepared.rows().size());
        int to = Math.min(from + size, prepared.rows().size());
        return new GenericReportResponse(prepared.summary(), prepared.columns(),
                select(prepared.rows().subList(from, to), prepared.keys()), page, size,
                prepared.rows().size(), pages(prepared.rows().size(), size));
    }

    @Transactional(readOnly = true)
    public EmployeeReportExportData exportData(ReportExportRequest request) {
        Prepared prepared = prepare(request.columns(), request.filters(), request.sort());
        List<Map<String, Object>> content = select(prepared.rows(), prepared.keys());
        for (int index = 0; index < content.size(); index++) {
            Map<String, Object> row = content.get(index);
            Object sowStatusLabel = prepared.rows().get(index).get("sowStatusLabel");
            if (row.containsKey("sowStatus") && sowStatusLabel != null) row.put("sowStatus", sowStatusLabel);
            Object milestoneStatus = row.get("milestoneStatus");
            if (milestoneStatus instanceof String code) {
                MILESTONE_STATUSES.stream().filter(option -> option.value().equalsIgnoreCase(code))
                        .findFirst().ifPresent(option -> row.put("milestoneStatus", option.label()));
            }
        }
        return new EmployeeReportExportData(prepared.columns(), content);
    }

    private Prepared prepare(List<String> requestedColumns, List<ReportFilterRequest> requestedFilters,
            List<ReportSortRequest> requestedSort) {
        List<String> keys = selectedColumns(requestedColumns);
        List<ReportFilterRequest> filters = requestedFilters == null ? List.of() : requestedFilters;
        List<ReportSortRequest> sorts = requestedSort == null || requestedSort.isEmpty()
                ? List.of(new ReportSortRequest("sowName", "ASC"),
                        new ReportSortRequest("milestoneStartDate", "ASC")) : requestedSort;
        validate(filters, sorts);
        List<Map<String, Object>> rows = flatten(sowRepository.findAllWithDetails()).stream()
                .filter(filterPredicate(filters)).sorted(comparator(sorts)).toList();
        Map<String, Long> summary = summary(rows);
        List<ReportResultColumn> columns = keys.stream().map(FIELDS::get)
                .map(field -> new ReportResultColumn(field.key(), field.label(), field.type())).toList();
        return new Prepared(keys, columns, rows, summary);
    }

    private List<Map<String, Object>> flatten(List<Sow> sows) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Sow sow : sows) {
            if (sow.getMilestones() == null || sow.getMilestones().isEmpty()) {
                rows.add(row(sow, null));
            } else {
                sow.getMilestones().stream()
                        .sorted(Comparator.comparing(SowMilestone::getDisplayOrder,
                                Comparator.nullsLast(Integer::compareTo)).thenComparing(SowMilestone::getId))
                        .forEach(milestone -> rows.add(row(sow, milestone)));
            }
        }
        return rows;
    }

    private Map<String, Object> row(Sow sow, SowMilestone milestone) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("sowId", sow.getId());
        row.put("sowNumber", sow.getCsxProjectId());
        row.put("sowName", sow.getSowName());
        row.put("clientId", sow.getClient() == null ? null : sow.getClient().getId());
        row.put("clientName", sow.getClient() == null ? null : sow.getClient().getClientName());
        row.put("businessUnitId", sow.getBusinessUnit() == null ? null : sow.getBusinessUnit().getId());
        row.put("businessUnitName", sow.getBusinessUnit() == null ? null : sow.getBusinessUnit().getName());
        row.put("sowStatus", sow.getStatus() == null ? null : sow.getStatus().getCode());
        row.put("sowStatusLabel", sow.getStatus() == null ? null : sow.getStatus().getName());
        row.put("sowStartDate", sow.getStartDate());
        row.put("sowEndDate", sow.getEndDate());
        row.put("hasMilestones", milestone != null);
        row.put("milestoneId", milestone == null ? null : milestone.getId());
        row.put("milestoneName", milestone == null ? null : milestone.getMilestoneName());
        row.put("milestoneStatus", milestone == null ? null : milestone.getStatus());
        row.put("milestoneStartDate", milestone == null ? null : milestone.getStartDate());
        row.put("milestoneEndDate", milestone == null ? null : milestone.getEndDate());
        row.put("plannedHours", milestone == null ? null : milestone.getEstimatedHours());
        int positionCount = milestone == null || milestone.getPositions() == null ? 0 : milestone.getPositions().size();
        row.put("positionCount", milestone == null ? null : positionCount);
        row.put("hasPositions", positionCount > 0);
        row.put("invoiceAmount", milestone == null ? null : milestone.getAmount());
        row.put("currency", currency(milestone));
        return row;
    }

    private String currency(SowMilestone milestone) {
        if (milestone == null || milestone.getPositions() == null) return null;
        Set<String> currencies = new LinkedHashSet<>();
        milestone.getPositions().forEach(position -> {
            if (position.getRateCard() != null && position.getRateCard().getCurrency() != null) {
                currencies.add(position.getRateCard().getCurrency().toUpperCase(Locale.ROOT));
            }
        });
        return currencies.size() == 1 ? currencies.iterator().next() : null;
    }

    private Predicate<Map<String, Object>> filterPredicate(List<ReportFilterRequest> filters) {
        return row -> filters.stream().allMatch(filter -> matches(row, filter));
    }

    private boolean matches(Map<String, Object> row, ReportFilterRequest filter) {
        Object actual = row.get(filter.field());
        String operator = filter.operator().toUpperCase(Locale.ROOT);
        if ("IS_EMPTY".equals(operator)) return actual == null || actual.toString().isBlank();
        if ("IN".equals(operator)) {
            List<String> expected = new ArrayList<>();
            filter.value().forEach(value -> expected.add(value.asText().trim().toUpperCase(Locale.ROOT)));
            return actual != null && expected.contains(actual.toString().toUpperCase(Locale.ROOT));
        }
        if (actual instanceof LocalDate date) {
            LocalDate expected = LocalDate.parse(filter.value().asText());
            return switch (operator) {
                case "BEFORE" -> date.isBefore(expected);
                case "AFTER" -> date.isAfter(expected);
                default -> date.equals(expected);
            };
        }
        if (actual instanceof Boolean bool) return bool == filter.value().asBoolean();
        String expected = filter.value().asText().trim();
        if ("CONTAINS".equals(operator)) {
            return actual != null && actual.toString().toLowerCase(Locale.ROOT)
                    .contains(expected.toLowerCase(Locale.ROOT));
        }
        return actual != null && actual.toString().equalsIgnoreCase(expected);
    }

    private Comparator<Map<String, Object>> comparator(List<ReportSortRequest> sorts) {
        Comparator<Map<String, Object>> result = null;
        for (ReportSortRequest sort : sorts) {
            Comparator<Map<String, Object>> next = Comparator.comparing(
                    row -> comparable(row.get(sort.field())), Comparator.nullsLast(Comparator.naturalOrder()));
            if ("DESC".equalsIgnoreCase(sort.direction())) next = next.reversed();
            result = result == null ? next : result.thenComparing(next);
        }
        Comparator<Map<String, Object>> stable = Comparator.comparing(row -> (Long) row.get("sowId"));
        return result == null ? stable : result.thenComparing(stable);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Comparable comparable(Object value) {
        if (value instanceof String text) return text.toLowerCase(Locale.ROOT);
        return value instanceof Comparable comparable ? comparable : value == null ? null : value.toString();
    }

    private Map<String, Long> summary(List<Map<String, Object>> rows) {
        Set<Object> sowIds = new HashSet<>();
        Set<Object> activeSowIds = new HashSet<>();
        long milestones = 0, hours = 0, positions = 0;
        for (Map<String, Object> row : rows) {
            sowIds.add(row.get("sowId"));
            if ("ACTIVE".equalsIgnoreCase(String.valueOf(row.get("sowStatus")))) activeSowIds.add(row.get("sowId"));
            if (row.get("milestoneId") != null) milestones++;
            if (row.get("plannedHours") instanceof Number number) hours += number.longValue();
            if (row.get("positionCount") instanceof Number number) positions += number.longValue();
        }
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("totalSows", (long) sowIds.size());
        summary.put("activeSows", (long) activeSowIds.size());
        summary.put("totalMilestones", milestones);
        summary.put("totalPlannedHours", hours);
        summary.put("totalPositions", positions);
        return summary;
    }

    private void validate(List<ReportFilterRequest> filters, List<ReportSortRequest> sorts) {
        for (ReportFilterRequest filter : filters) {
            Field field = FIELDS.get(filter.field());
            if (field == null || !field.filterable()) throw invalidField(filter.field());
            String operator = filter.operator().toUpperCase(Locale.ROOT);
            if (!field.operators().contains(operator)) {
                throw new InvalidOperationException("Operator " + operator + " is not allowed for " + filter.field());
            }
            if (!"IS_EMPTY".equals(operator) && (filter.value() == null || filter.value().isNull())) {
                throw new InvalidOperationException("Filter value is required");
            }
            if ("IN".equals(operator) && (!filter.value().isArray() || filter.value().isEmpty())) {
                throw new InvalidOperationException("IN requires a non-empty array value");
            }
            if ("DATE".equals(field.type()) && !"IS_EMPTY".equals(operator)) {
                try { LocalDate.parse(filter.value().asText()); }
                catch (RuntimeException ex) { throw new InvalidOperationException(filter.field() + " requires an ISO date (yyyy-MM-dd)"); }
            }
        }
        for (ReportSortRequest sort : sorts) {
            Field field = FIELDS.get(sort.field());
            if (field == null || !field.sortable()) throw new InvalidOperationException("Field is not sortable: " + sort.field());
            if (!Set.of("ASC", "DESC").contains(sort.direction().toUpperCase(Locale.ROOT))) {
                throw new InvalidOperationException("Sort direction must be ASC or DESC");
            }
        }
    }

    private List<String> selectedColumns(List<String> requested) {
        List<String> keys = requested == null || requested.isEmpty()
                ? FIELDS.values().stream().filter(Field::defaultVisible).filter(Field::selectable).map(Field::key).toList()
                : requested;
        LinkedHashSet<String> unique = new LinkedHashSet<>(keys);
        for (String key : unique) {
            Field field = FIELDS.get(key);
            if (field == null || !field.selectable()) throw invalidField(key);
        }
        return List.copyOf(unique);
    }

    private List<Map<String, Object>> select(List<Map<String, Object>> rows, List<String> keys) {
        return rows.stream().map(source -> {
            Map<String, Object> selected = new LinkedHashMap<>();
            keys.forEach(key -> selected.put(key, source.get(key)));
            return selected;
        }).toList();
    }

    private int pages(int elements, int size) { return elements == 0 ? 0 : (elements + size - 1) / size; }
    private InvalidOperationException invalidField(String field) { return new InvalidOperationException("Unsupported report field: " + field); }

    private static LinkedHashMap<String, Field> fields() {
        LinkedHashMap<String, Field> map = new LinkedHashMap<>();
        add(map,"sowNumber","SOW Number","TEXT",null,true,true,true,true,TEXT,null);
        add(map,"sowName","SOW","TEXT",null,true,true,true,true,TEXT,null);
        add(map,"clientId","Client","LOOKUP","CLIENT",false,false,false,true,LOOKUP,null);
        add(map,"clientName","Client","TEXT",null,true,true,true,false,List.of(),null);
        add(map,"businessUnitId","Business Unit","LOOKUP","DEPARTMENT",false,false,false,true,LOOKUP,null);
        add(map,"businessUnitName","Business Unit","TEXT",null,true,true,true,false,List.of(),null);
        add(map,"sowStatus","SOW Status","LOOKUP","SOW_STATUS",true,true,true,true,LOOKUP,null);
        add(map,"sowStartDate","SOW Start Date","DATE",null,true,true,true,true,DATE,null);
        add(map,"sowEndDate","SOW End Date","DATE",null,true,true,true,true,DATE,null);
        add(map,"milestoneName","Milestone","TEXT",null,true,true,true,true,TEXT,null);
        add(map,"milestoneStatus","Milestone Status","ENUM",null,true,true,true,true,LOOKUP,MILESTONE_STATUSES);
        add(map,"milestoneStartDate","Milestone Start Date","DATE",null,true,true,true,true,DATE,null);
        add(map,"milestoneEndDate","Milestone End Date","DATE",null,true,true,true,true,DATE,null);
        add(map,"plannedHours","Planned Hours","NUMBER",null,true,true,true,false,List.of(),null);
        add(map,"positionCount","Position Count","NUMBER",null,true,true,true,false,List.of(),null);
        add(map,"invoiceAmount","Invoice Amount","CURRENCY",null,true,true,true,false,List.of(),null);
        add(map,"currency","Currency","TEXT",null,true,true,true,true,LOOKUP,null);
        add(map,"hasPositions","Has Positions","BOOLEAN",null,false,false,false,true,BOOLEAN,List.of(new ReportOption("true","Yes"),new ReportOption("false","No")));
        add(map,"hasMilestones","Has Milestones","BOOLEAN",null,false,false,false,true,BOOLEAN,List.of(new ReportOption("true","Yes"),new ReportOption("false","No")));
        return map;
    }

    private static void add(Map<String, Field> map, String key, String label, String type, String lookup,
            boolean selectable, boolean defaultVisible, boolean sortable, boolean filterable,
            List<String> operators, List<ReportOption> options) {
        map.put(key, new Field(key,label,type,lookup,selectable,defaultVisible,sortable,filterable,operators,options));
    }

    private record Prepared(List<String> keys, List<ReportResultColumn> columns,
            List<Map<String,Object>> rows, Map<String,Long> summary) {}
    private record Field(String key,String label,String type,String lookupCode,boolean selectable,
            boolean defaultVisible,boolean sortable,boolean filterable,List<String> operators,List<ReportOption> options) {
        ReportColumnDefinition definition() {
            return new ReportColumnDefinition(key,label,type,lookupCode,selectable,defaultVisible,
                    sortable,filterable,operators,options);
        }
    }
}
