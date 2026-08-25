package com.rit.performance.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowResourceAssignmentResponse {
    private Long assignmentId;
    private Long employeeId;
    private String employeeName;
}
