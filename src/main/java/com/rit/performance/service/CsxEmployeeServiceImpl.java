package com.rit.performance.service;

import com.rit.performance.dto.CsxEmployeeResponse;
import com.rit.performance.dto.CsxEmployeeCreateRequest;
import com.rit.performance.dto.CsxEmployeeUpdateRequest;
import com.rit.performance.entity.CsxEmployee;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.mapper.CsxEmployeeMapper;
import com.rit.performance.repository.CsxEmployeeRepository;
import com.rit.performance.repository.LookupValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CsxEmployeeServiceImpl implements CsxEmployeeService {
    private final CsxEmployeeRepository repository;
    private final LookupValueRepository lookupValueRepository;

    @Override
    @Transactional
    public CsxEmployeeResponse create(CsxEmployeeCreateRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (email != null && repository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("CSX employee email already exists: " + email);
        }

        CsxEmployee employee = new CsxEmployee();
        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(trimToNull(request.getLastName()));
        employee.setEmail(email);
        employee.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        employee.setDesignation(findDesignation(request.getDesignationId()));
        employee.setBusinessUnit(findBusinessUnit(request.getBusinessUnitId()));
        employee.setStatus(normalizeStatus(request.getStatus()));
        return CsxEmployeeMapper.toResponse(repository.save(employee));
    }

    @Override
    @Transactional
    public CsxEmployeeResponse update(Long id, CsxEmployeeUpdateRequest request) {
        if (request.getId() != null && !id.equals(request.getId())) {
            throw new InvalidOperationException(
                    "Request id " + request.getId() + " does not match path id " + id);
        }

        CsxEmployee employee = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CSX employee not found: " + id));
        String email = normalizeEmail(request.getEmail());
        if (email != null && repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateResourceException("CSX employee email already exists: " + email);
        }

        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(trimToNull(request.getLastName()));
        employee.setEmail(email);
        employee.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        employee.setDesignation(findDesignation(request.getDesignationId()));
        employee.setBusinessUnit(findBusinessUnit(request.getBusinessUnitId()));
        employee.setStatus(normalizeStatus(request.getStatus()));
        return CsxEmployeeMapper.toResponse(repository.save(employee));
    }

    @Override
    public List<CsxEmployeeResponse> getAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "firstName", "lastName")).stream()
                .map(CsxEmployeeMapper::toResponse)
                .toList();
    }

    @Override
    public CsxEmployeeResponse getById(Long id) {
        CsxEmployee employee = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CSX employee not found: " + id));
        return CsxEmployeeMapper.toResponse(employee);
    }

    private LookupValue findBusinessUnit(Long id) {
        if (id == null) return null;
        return lookupValueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business unit not found: " + id));
    }

    private LookupValue findDesignation(Long id) {
        if (id == null) return null;
        LookupValue designation = lookupValueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found: " + id));
        if (designation.getLookupType() == null
                || !"DESIGNATION".equalsIgnoreCase(designation.getLookupType().getCode())) {
            throw new InvalidOperationException(
                    "Lookup value " + id + " is not a designation");
        }
        return designation;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "ACTIVE";
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized)) {
            throw new InvalidOperationException("status must be ACTIVE or INACTIVE");
        }
        return normalized;
    }

    private String normalizeEmail(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
