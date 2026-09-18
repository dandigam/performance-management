package com.rit.performance.dto.response;

import lombok.Builder;
import lombok.Value;
import java.time.LocalDate;

@Value
@Builder
public class SowSummaryResponse {
    Long sowId;
    String sowName;
    Long businessUnitId;
    String businessUnitName;
    Long pocEmployeeId;
    String pocEmployeeName;
    LocalDate startDate;
    LocalDate endDate;
    String status;
    int totalPositionCount;
    int openPositionCount;
}
