package com.rit.performance.dto.response;

import com.rit.performance.entity.TimesheetEmployeeProjectStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class TimesheetEmployeeProjectResponse {
    private Long timesheetEmployeeProjectId;
    private Long employeeId;
    private String employeeName;
    private Long sowId;
    private String sowName;
    private String designationName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxHoursPerDay;
    private Long level1ApproverId;
    private String level1ApproverName;
    private Long level2ApproverId;
    private String level2ApproverName;
    private TimesheetEmployeeProjectStatus status;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
}
