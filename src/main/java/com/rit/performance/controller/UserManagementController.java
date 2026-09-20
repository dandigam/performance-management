package com.rit.performance.controller;

import com.rit.performance.dto.UserManagementUserResponse;
import com.rit.performance.dto.UserStatusUpdateRequest;
import com.rit.performance.dto.UserRoleUpdateRequest;
import com.rit.performance.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user-management")
@RequiredArgsConstructor
public class UserManagementController {
    private final UserManagementService userManagementService;

    @GetMapping("/users")
    public ResponseEntity<List<UserManagementUserResponse>> getUsers() {
        return ResponseEntity.ok(userManagementService.getUsers());
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<UserManagementUserResponse> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        return ResponseEntity.ok(userManagementService.updateStatus(userId, request.status()));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<UserManagementUserResponse> updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleUpdateRequest request) {
        return ResponseEntity.ok(userManagementService.updateRole(userId, request.roleId()));
    }
}
