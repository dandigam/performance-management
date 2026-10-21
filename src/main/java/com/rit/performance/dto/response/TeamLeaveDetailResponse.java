package com.rit.performance.dto.response;

import com.rit.performance.entity.LeaveApprovalLevel;
import com.rit.performance.entity.LeaveRequestStatus;
import com.rit.performance.entity.LeaveUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record TeamLeaveDetailResponse(Long requestId, Long employeeId, String employeeName,
        String employeeEmail, Long leaveTypeId, String leaveType, LeaveUnit unit,
        LocalDate fromDate, LocalDate toDate, BigDecimal totalHours,
        Object currentAvailableBalance, Map<Integer, Object> availableByYear,
        String reason, String notes, LocalDateTime submittedAt, LeaveRequestStatus status,
        LeaveApprovalLevel approvalLevel, List<LeaveRequestDayResponse> days,
        List<LeaveApprovalHistoryResponse> approvalHistory) {}
