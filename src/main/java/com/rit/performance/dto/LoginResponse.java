package com.rit.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private Instant expiresAt;
    private Long userId;
    private String username;
    private Long roleId;
    private String roleName;
    private Long employeeId;
    private String employeeName;
    private String status;
}
