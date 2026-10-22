package com.rit.performance.service;

import com.rit.performance.dto.request.EmployeeOrgHierarchyRequest;
import com.rit.performance.dto.request.EmployeeOrgHierarchyBulkRequest;
import com.rit.performance.dto.response.EmployeeOrgHierarchyResponse;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeOrgHierarchy;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.EmployeeOrgHierarchyRepository;
import com.rit.performance.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeOrgHierarchyService {
    private final EmployeeOrgHierarchyRepository repository;
    private final EmployeeRepository employees;

    public List<EmployeeOrgHierarchyResponse> getAll() {
        return repository.findAllByOrderByIdAsc().stream().map(this::response).toList();
    }

    public EmployeeOrgHierarchyResponse get(Long id) {
        return response(find(id));
    }

    public EmployeeOrgHierarchyResponse getByEmployee(Long employeeId) {
        return response(repository.findByEmployee_Id(employeeId).orElseThrow(() ->
                new ResourceNotFoundException("Organization reporting not found for employee: " + employeeId)));
    }

    @Transactional
    public List<EmployeeOrgHierarchyResponse> createBulk(EmployeeOrgHierarchyBulkRequest request) {
        Set<Long> unique = new HashSet<>();
        for (var entry : request.getEmployeeList()) {
            if (!unique.add(entry.getEmployeeId())) {
                throw new InvalidOperationException("employeeList cannot contain duplicate employee IDs");
            }
        }
        repository.lockHierarchyWrites(PageRequest.of(0, 1));
        List<EmployeeOrgHierarchyResponse> result = new java.util.ArrayList<>();
        for (var entry : request.getEmployeeList()) {
            EmployeeOrgHierarchyRequest single = new EmployeeOrgHierarchyRequest();
            single.setEmployeeId(entry.getEmployeeId());
            single.setReportingManagerId(request.getReportingManagerId());
            EmployeeOrgHierarchy hierarchy = new EmployeeOrgHierarchy();
            apply(hierarchy, single);
            result.add(response(repository.saveAndFlush(hierarchy)));
        }
        return result;
    }

    @Transactional
    public EmployeeOrgHierarchyResponse create(EmployeeOrgHierarchyRequest request) {
        repository.lockHierarchyWrites(PageRequest.of(0, 1));
        EmployeeOrgHierarchy hierarchy = new EmployeeOrgHierarchy();
        apply(hierarchy, request);
        return response(repository.saveAndFlush(hierarchy));
    }

    @Transactional
    public EmployeeOrgHierarchyResponse update(Long id, EmployeeOrgHierarchyRequest request) {
        repository.lockHierarchyWrites(PageRequest.of(0, 1));
        EmployeeOrgHierarchy hierarchy = find(id);
        if (!hierarchy.getEmployee().getId().equals(request.getEmployeeId())) {
            throw new InvalidOperationException("employeeId cannot be changed; update the reporting manager instead");
        }
        apply(hierarchy, request);
        return response(repository.saveAndFlush(hierarchy));
    }

    @Transactional
    public void delete(Long id) {
        repository.lockHierarchyWrites(PageRequest.of(0, 1));
        repository.delete(find(id));
    }

    private void apply(EmployeeOrgHierarchy hierarchy, EmployeeOrgHierarchyRequest request) {
        Employee employee = employee(request.getEmployeeId());
        Employee manager = request.getReportingManagerId() == null ? null : employee(request.getReportingManagerId());
        repository.findByEmployee_Id(employee.getId()).filter(existing -> !existing.getId().equals(hierarchy.getId()))
                .ifPresent(existing -> { throw new DuplicateResourceException("Organization reporting already exists for employee: " + employee.getId()); });
        Map<Long, Long> chain = new HashMap<>();
        repository.findAll().forEach(row -> {
            if (row.getReportingManager() != null) chain.put(row.getEmployee().getId(), row.getReportingManager().getId());
        });
        if (manager == null) chain.remove(employee.getId());
        else chain.put(employee.getId(), manager.getId());
        Set<Long> visited = new HashSet<>();
        Long current = employee.getId();
        while (current != null) {
            if (!visited.add(current)) throw new InvalidOperationException("Reporting relationship would create a circular hierarchy");
            current = chain.get(current);
        }
        hierarchy.setEmployee(employee);
        hierarchy.setReportingManager(manager);
    }

    private Employee employee(Long id) {
        return employees.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    private EmployeeOrgHierarchy find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Organization reporting not found: " + id));
    }

    private EmployeeOrgHierarchyResponse response(EmployeeOrgHierarchy row) {
        Employee manager = row.getReportingManager();
        return new EmployeeOrgHierarchyResponse(row.getId(), row.getEmployee().getId(), name(row.getEmployee()),
                manager == null ? null : manager.getId(), manager == null ? null : name(manager),
                row.getCreatedBy(), row.getCreatedOn(), row.getUpdatedBy(), row.getUpdatedOn());
    }

    private String name(Employee employee) {
        return (java.util.Objects.toString(employee.getFirstName(), "") + " "
                + java.util.Objects.toString(employee.getLastName(), "")).trim();
    }
}
