package com.rit.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeCurrentProjectResponse {
    private Long projectId;
    private String projectName;
    private Long sowId;
    private String sowName;
    private String designationName;
}
