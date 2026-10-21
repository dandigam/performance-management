package com.rit.performance.dto.request;

import com.rit.performance.entity.TimesheetApprovalStatus;

public record TimesheetApprovalRequest(TimesheetApprovalStatus status, String comments) {}
