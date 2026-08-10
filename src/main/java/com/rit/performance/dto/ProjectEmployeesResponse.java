package com.rit.performance.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectEmployeesResponse {
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Long departmentId;
    private String departmentName;
    private List<ProjectEmployeeResponse> employees;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
}
