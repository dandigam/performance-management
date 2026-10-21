package com.rit.performance.dto.response;

import com.rit.performance.entity.LeaveUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record LeaveScheduleResponse(Long employeeLeavePolicyId, Long leaveTypeId,
        String leaveTypeCode, String leaveTypeName, LeaveUnit unit,
        Object availableBalance, Map<Integer, Object> availableByYear, boolean unlimited,
        LocalDate fromDate, LocalDate toDate,
        List<LeaveRequestDayResponse> days) {}
