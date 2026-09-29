package com.rit.performance.service;

import com.rit.performance.dto.UserManagementUserResponse;
import com.rit.performance.dto.UserCreateRequest;

import java.util.List;

public interface UserManagementService {
    List<UserManagementUserResponse> getUsers();

    UserManagementUserResponse createUser(UserCreateRequest request);

    UserManagementUserResponse updateStatus(Long userId, String status);

    UserManagementUserResponse updateRole(Long userId, Long roleId);

    void updatePortalAccess(Long userId, String portalAccess);
}
