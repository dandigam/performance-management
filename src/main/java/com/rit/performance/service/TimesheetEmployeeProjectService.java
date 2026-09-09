package com.rit.performance.service;

import com.rit.performance.dto.request.TimesheetEmployeeProjectRequest;
import com.rit.performance.dto.response.TimesheetEmployeeProjectResponse;

import java.util.List;

public interface TimesheetEmployeeProjectService {
    List<TimesheetEmployeeProjectResponse> create(
            Long employeeId, List<TimesheetEmployeeProjectRequest> requests);

    List<TimesheetEmployeeProjectResponse> update(
            Long employeeId, List<TimesheetEmployeeProjectRequest> requests);

    List<TimesheetEmployeeProjectResponse> getAll(Long employeeId);
}
