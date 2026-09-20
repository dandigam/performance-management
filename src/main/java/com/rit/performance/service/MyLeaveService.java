package com.rit.performance.service;

import com.rit.performance.dto.request.*;
import com.rit.performance.dto.response.*;
import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyLeaveService {
    private static final List<LeaveRequestStatus> BLOCKING_STATUSES = List.of(
            LeaveRequestStatus.SUBMITTED, LeaveRequestStatus.LEVEL1_APPROVED, LeaveRequestStatus.APPROVED);
    private final CurrentEmployeeService currentEmployee;
    private final EmployeeRepository employees;
    private final EmployeeLeavePolicyRepository assignments;
    private final LeavePolicyRuleRepository rules;
    private final TimesheetEmployeeProjectDayRepository schedules;
    private final EmployeeLeaveBalanceRepository balances;
    private final EmployeeLeaveBalanceAdjustmentRepository adjustments;
    private final EmployeeLeaveBalanceService balanceService;
    private final LeaveRequestRepository requests;

    public List<EmployeeLeaveBalanceResponse> myBalances(int year) {
        return balanceService.getForEmployee(currentEmployee.currentEmployee().getId(), year);
    }

    public List<LeaveRequestResponse> myRequests() {
        return requests.findByEmployeeIdOrderByCreatedOnDescIdDesc(currentEmployee.currentEmployee().getId())
                .stream().map(this::response).toList();
    }

    public LeaveRequestResponse myRequest(Long id) {
        return response(findMine(currentEmployee.currentEmployee().getId(), id));
    }

    public LeaveScheduleResponse schedule(Long leaveTypeId, LocalDate from, LocalDate to) {
        Long employeeId = currentEmployee.currentEmployee().getId();
        validateDates(from, to);
        EmployeeLeavePolicy assignment = coveringAssignment(employeeId, from, to);
        LeavePolicyRule rule = activeRule(assignment, leaveTypeId);
        Map<LocalDate, BigDecimal> configured = scheduledHours(employeeId, from, to);
        if (configured.isEmpty()) throw new InvalidOperationException("No configured working dates in this range.");
        Map<Integer, Object> availableByYear = new LinkedHashMap<>();
        for (int year = from.getYear(); year <= to.getYear(); year++)
            availableByYear.put(year, balanceAvailability(assignment, rule, year));
        List<LeaveRequestDayResponse> days = configured.entrySet().stream()
                .map(entry -> new LeaveRequestDayResponse(null, entry.getKey(), entry.getValue(), entry.getValue()))
                .toList();
        LeaveType type = rule.getLeaveType();
        boolean unlimited = "Unlimited".equals(availableByYear.get(from.getYear()));
        return new LeaveScheduleResponse(assignment.getId(), type.getId(), type.getCode(), type.getName(),
                type.getUnit(), availableByYear.get(from.getYear()), availableByYear,
                unlimited, from, to, days);
    }

    @Transactional
    public LeaveRequestResponse createDraft(LeaveRequestDraftRequest input) {
        Employee employee = currentEmployee.currentEmployee();
        Prepared prepared = prepare(employee.getId(), input);
        validateNoOverlap(employee.getId(), -1L, prepared);
        validateBalances(prepared);
        LeaveRequest request = new LeaveRequest();
        request.setEmployee(employee);
        apply(request, input, prepared);
        return response(requests.saveAndFlush(request));
    }

    @Transactional
    public LeaveRequestResponse updateDraft(Long id, LeaveRequestDraftRequest input) {
        Long employeeId = currentEmployee.currentEmployee().getId();
        LeaveRequest request = findMine(employeeId, id);
        requireDraft(request);
        Prepared prepared = prepare(employeeId, input);
        validateNoOverlap(employeeId, id, prepared);
        validateBalances(prepared);
        request.getDays().clear();
        requests.flush();
        apply(request, input, prepared);
        return response(requests.saveAndFlush(request));
    }

    @Transactional
    public LeaveRequestResponse submit(Long id) {
        Long employeeId = currentEmployee.currentEmployee().getId();
        lockEmployee(employeeId);
        LeaveRequest request = findMine(employeeId, id);
        requireDraft(request);
        List<LeaveRequestDayRequest> days = request.getDays().stream()
                .map(day -> new LeaveRequestDayRequest(day.getLeaveDate(), day.getRequestedHours())).toList();
        LeaveRequestDraftRequest input = new LeaveRequestDraftRequest(request.getLeaveType().getId(),
                request.getFromDate(), request.getToDate(), request.getReason(), request.getNotes(), days);
        Prepared prepared = prepare(employeeId, input);
        validateNoOverlap(employeeId, id, prepared);
        validateBalances(prepared);
        request.getDays().clear();
        requests.flush();
        apply(request, input, prepared);
        EmployeeLeavePolicy assignment = prepared.assignment();
        if (assignment.getLevel1Approver() == null)
            throw new InvalidOperationException("Level 1 leave approver is not configured on the policy assignment.");
        request.setLevel1Approver(assignment.getLevel1Approver());
        request.setLevel2Approver(assignment.getLevel2Approver());
        request.setStatus(LeaveRequestStatus.SUBMITTED);
        request.setSubmittedAt(LocalDateTime.now());
        return response(requests.saveAndFlush(request));
    }

    @Transactional
    public LeaveRequestResponse cancel(Long id) {
        Long employeeId = currentEmployee.currentEmployee().getId();
        LeaveRequest request = requests.findByIdForApproval(id)
                .filter(item -> item.getEmployee().getId().equals(employeeId))
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + id));
        if (request.getStatus() != LeaveRequestStatus.SUBMITTED)
            throw new InvalidOperationException("Only a submitted leave request can be cancelled.");
        request.setStatus(LeaveRequestStatus.CANCELLED);
        return response(requests.saveAndFlush(request));
    }

    private Prepared prepare(Long employeeId, LeaveRequestDraftRequest input) {
        validateDates(input.fromDate(), input.toDate());
        EmployeeLeavePolicy assignment = coveringAssignment(employeeId, input.fromDate(), input.toDate());
        LeavePolicyRule rule = activeRule(assignment, input.leaveTypeId());
        Map<LocalDate, BigDecimal> configured = scheduledHours(employeeId, input.fromDate(), input.toDate());
        if (configured.isEmpty()) throw new InvalidOperationException("No configured working dates in this range.");
        if (input.days() == null || input.days().isEmpty())
            throw new InvalidOperationException("At least one requested working date is required.");
        Set<LocalDate> seen = new HashSet<>();
        List<PreparedDay> preparedDays = new ArrayList<>();
        for (LeaveRequestDayRequest day : input.days()) {
            if (day == null || day.leaveDate() == null || day.requestedHours() == null
                    || day.requestedHours().signum() <= 0)
                throw new InvalidOperationException("Each requested date needs positive requested hours.");
            if (!seen.add(day.leaveDate())) throw new InvalidOperationException("Duplicate requested leave date.");
            BigDecimal scheduled = configured.get(day.leaveDate());
            if (scheduled == null || day.requestedHours().compareTo(scheduled) > 0)
                throw new InvalidOperationException("Requested hours exceed configured scheduled hours or date is not working: " + day.leaveDate());
            preparedDays.add(new PreparedDay(day.leaveDate(), scheduled, day.requestedHours()));
        }
        preparedDays.sort(Comparator.comparing(PreparedDay::date));
        return new Prepared(assignment, rule, preparedDays);
    }

    private void validateDates(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from))
            throw new InvalidOperationException("fromDate must be on or before toDate.");
    }

    private EmployeeLeavePolicy coveringAssignment(Long employeeId, LocalDate from, LocalDate to) {
        return assignments.findCovering(employeeId, from, to, LeavePolicyStatus.ACTIVE).stream()
                .findFirst().orElseThrow(() -> new InvalidOperationException(
                        "Dates must be within an active leave policy assignment."));
    }

    private LeavePolicyRule activeRule(EmployeeLeavePolicy assignment, Long leaveTypeId) {
        LeavePolicyRule rule = rules.findByLeavePolicyIdAndLeaveTypeIdAndStatus(
                assignment.getLeavePolicy().getId(), leaveTypeId, LeavePolicyStatus.ACTIVE)
                .orElseThrow(() -> new InvalidOperationException("Leave type is not active in the assigned policy."));
        if (rule.getLeaveType().getStatus() != LeaveTypeStatus.ACTIVE)
            throw new InvalidOperationException("Leave type is inactive.");
        return rule;
    }

    private Map<LocalDate, BigDecimal> scheduledHours(Long employeeId, LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> hours = new TreeMap<>();
        for (TimesheetEmployeeProjectDay day : schedules.findLeaveSchedule(employeeId, from, to,
                TimesheetScheduleStatus.ACTIVE, TimesheetEmployeeProjectStatus.ACTIVE)) {
            if (day.getScheduledHours().signum() > 0)
                hours.merge(day.getWorkDate(), day.getScheduledHours(), BigDecimal::add);
        }
        return hours;
    }

    private Object balanceAvailability(EmployeeLeavePolicy assignment, LeavePolicyRule rule, int year) {
        EmployeeLeaveBalance balance = balance(assignment, rule, year);
        return balance.getEntitled() == null ? "Unlimited" : available(balance);
    }

    private EmployeeLeaveBalance balance(EmployeeLeavePolicy assignment, LeavePolicyRule rule, int year) {
        EmployeeLeaveBalance balance = balances.findByEmployeeLeavePolicyIdAndLeaveTypeIdAndBalanceYear(
                assignment.getId(), rule.getLeaveType().getId(), year)
                .orElseThrow(() -> new InvalidOperationException("Initialize the leave balance for year " + year + " first."));
        if (balance.getStatus() != LeavePolicyStatus.ACTIVE)
            throw new InvalidOperationException("Leave balance is inactive for year " + year + ".");
        return balance;
    }

    private BigDecimal available(EmployeeLeaveBalance balance) {
        BigDecimal adjustmentsTotal = adjustments.findByEmployeeLeaveBalanceIdOrderByAdjustmentDateAscIdAsc(balance.getId())
                .stream().map(a -> a.getAdjustmentType() == LeaveBalanceAdjustmentType.ADD
                        ? a.getAmount() : a.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return balance.getOpeningBalance().add(balance.getEntitled())
                .add(adjustmentsTotal).subtract(balance.getUsed());
    }

    private void validateBalances(Prepared prepared) {
        Map<Integer, List<PreparedDay>> byYear = prepared.days().stream()
                .collect(Collectors.groupingBy(day -> day.date().getYear()));
        for (var entry : byYear.entrySet()) {
            EmployeeLeaveBalance balance = balance(prepared.assignment(), prepared.rule(), entry.getKey());
            if (balance.getEntitled() == null) continue;
            BigDecimal requested = entry.getValue().stream().map(day ->
                    prepared.rule().getLeaveType().getUnit() == LeaveUnit.HOURS
                            ? day.requested() : day.requested().divide(day.scheduled(), 6, RoundingMode.HALF_UP))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (requested.compareTo(available(balance)) > 0)
                throw new InvalidOperationException("Requested leave exceeds available balance for year " + entry.getKey() + ".");
        }
    }

    private void validateNoOverlap(Long employeeId, Long excludedId, Prepared prepared) {
        if (!requests.findBlockingRequests(employeeId, excludedId, BLOCKING_STATUSES,
                prepared.days().stream().map(PreparedDay::date).toList()).isEmpty())
            throw new InvalidOperationException("A submitted or approved leave request already covers one of these dates.");
    }

    private void apply(LeaveRequest request, LeaveRequestDraftRequest input, Prepared prepared) {
        request.setEmployeeLeavePolicy(prepared.assignment());
        request.setLeaveType(prepared.rule().getLeaveType());
        request.setFromDate(input.fromDate());
        request.setToDate(input.toDate());
        request.setReason(input.reason().trim());
        request.setNotes(input.notes());
        BigDecimal total = BigDecimal.ZERO;
        for (PreparedDay day : prepared.days()) {
            LeaveRequestDay item = new LeaveRequestDay();
            item.setLeaveRequest(request);
            item.setLeaveDate(day.date());
            item.setScheduledHours(day.scheduled());
            item.setRequestedHours(day.requested());
            request.getDays().add(item);
            total = total.add(day.requested());
        }
        request.setTotalHours(total);
    }

    private LeaveRequest findMine(Long employeeId, Long id) {
        return requests.findByIdAndEmployeeId(id, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + id));
    }

    private void requireDraft(LeaveRequest request) {
        if (request.getStatus() != LeaveRequestStatus.DRAFT)
            throw new InvalidOperationException("Only a draft leave request can be edited or submitted.");
    }

    private void lockEmployee(Long employeeId) {
        employees.findByIdForLeavePolicyUpdate(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
    }

    private LeaveRequestResponse response(LeaveRequest request) {
        LeaveType type = request.getLeaveType();
        return new LeaveRequestResponse(request.getId(), request.getEmployeeLeavePolicy().getId(),
                type.getId(), type.getCode(), type.getName(), type.getUnit(), request.getFromDate(),
                request.getToDate(), request.getTotalHours(), request.getReason(), request.getNotes(),
                request.getStatus(), request.getSubmittedAt(), request.getCreatedOn(), request.getCreatedBy(),
                request.getUpdatedOn(), request.getUpdatedBy(), request.getDays().stream()
                        .sorted(Comparator.comparing(LeaveRequestDay::getLeaveDate))
                        .map(day -> new LeaveRequestDayResponse(day.getId(), day.getLeaveDate(),
                                day.getScheduledHours(), day.getRequestedHours())).toList());
    }

    private record Prepared(EmployeeLeavePolicy assignment, LeavePolicyRule rule, List<PreparedDay> days) {}
    private record PreparedDay(LocalDate date, BigDecimal scheduled, BigDecimal requested) {}
}
