package com.rit.performance.service;

import com.rit.performance.dto.UserManagementUserResponse;

import java.util.List;

public interface UserManagementService {
    List<UserManagementUserResponse> getUsers();

    UserManagementUserResponse updateStatus(Long userId, String status);

    UserManagementUserResponse updateRole(Long userId, Long roleId);
}
