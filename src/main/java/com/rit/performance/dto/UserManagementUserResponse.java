package com.rit.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserManagementUserResponse {
    private Long userId;
    private String username;
    private String email;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private String departmentName;
    private String status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
