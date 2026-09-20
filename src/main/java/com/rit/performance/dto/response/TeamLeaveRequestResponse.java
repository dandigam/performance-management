package com.rit.performance.dto.response;

import com.rit.performance.entity.LeaveApprovalLevel;
import com.rit.performance.entity.LeaveRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TeamLeaveRequestResponse(Long requestId, Long employeeId, String employeeName,
        String leaveType, LocalDate fromDate, LocalDate toDate, BigDecimal totalHours,
        LocalDateTime submittedAt, Object currentAvailableBalance,
        LeaveApprovalLevel approvalLevel, LeaveRequestStatus status) {}
