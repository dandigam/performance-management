package com.rit.performance.dto.response;

import lombok.Builder;
import lombok.Value;
import java.time.LocalDate;

@Value
@Builder
public class SowSummaryResponse {
    Long sowId;
    String sowName;
    Long clientId;
    String clientName;
    Long businessUnitId;
    String businessUnitName;
    Long pocEmployeeId;
    String pocEmployeeName;
    Long deliveryOwnerEmployeeId;
    String deliveryOwnerEmployeeName;
    Long technicalLeadEmployeeId;
    String technicalLeadEmployeeName;
    LocalDate startDate;
    LocalDate actualStartDate;
    LocalDate endDate;
    String status;
    int totalPositionCount;
    int openPositionCount;
}
