package com.rit.performance.dto.request;

public record ChangePasswordRequest(String currentPassword, String newPassword) {}
