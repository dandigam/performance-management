package com.rit.performance.service;

import com.rit.performance.entity.Projects;
import com.rit.performance.dto.ProjectEmployeesResponse;
import com.rit.performance.dto.ProjectEmployeeCreateRequest;
import com.rit.performance.dto.ProjectEmployeeResponse;
import com.rit.performance.dto.ProjectEmployeeStatusUpdateRequest;

import java.util.List;

public interface ProjectsService {

    Projects save(Projects project);

    Projects update(Long id, Projects project);

    Projects getById(Long id);

    List<Projects> getAll();

    ProjectEmployeesResponse getEmployees(Long projectId, int page, int size);

    ProjectEmployeeResponse addEmployee(Long projectId, ProjectEmployeeCreateRequest request);

    ProjectEmployeeResponse updateEmployeeAssignmentStatus(
            Long projectId, Long assignmentId, ProjectEmployeeStatusUpdateRequest request);

    void delete(Long id);
}
