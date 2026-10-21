package com.rit.performance.service;

import com.rit.performance.dto.request.LeaveTypeRequest;
import com.rit.performance.dto.response.LeaveTypeResponse;
import com.rit.performance.entity.*;
import com.rit.performance.exception.*;
import com.rit.performance.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveTypeService {
    private final LeaveTypeRepository repository;

    @Transactional
    public LeaveTypeResponse create(LeaveTypeRequest request) {
        String code = normalize(request.code());
        if (repository.existsByCodeIgnoreCase(code)) throw duplicate();
        LeaveType type = new LeaveType();
        apply(type, request, code);
        return persist(type);
    }

    public List<LeaveTypeResponse> getAll(LeaveTypeStatus status) {
        return (status == null ? repository.findAllByOrderByNameAscIdAsc()
                : repository.findByStatusOrderByNameAscIdAsc(status)).stream().map(this::response).toList();
    }

    public LeaveTypeResponse getById(Long id) { return response(find(id)); }

    @Transactional
    public LeaveTypeResponse update(Long id, LeaveTypeRequest request) {
        LeaveType type = find(id);
        String code = normalize(request.code());
        if (repository.existsByCodeIgnoreCaseAndIdNot(code, id)) throw duplicate();
        apply(type, request, code);
        return persist(type);
    }

    @Transactional
    public LeaveTypeResponse changeStatus(Long id, LeaveTypeStatus status) {
        LeaveType type = find(id);
        type.setStatus(status);
        return response(repository.saveAndFlush(type));
    }

    private LeaveType find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave type not found: " + id));
    }

    private String normalize(String code) { return code.trim().toUpperCase(Locale.ROOT); }

    private void apply(LeaveType type, LeaveTypeRequest request, String code) {
        type.setCode(code);
        type.setName(request.name().trim());
        type.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
        type.setUnit(request.unit());
        type.setPaid(request.paid());
        if (request.status() != null) type.setStatus(request.status());
    }

    private LeaveTypeResponse persist(LeaveType type) {
        try { return response(repository.saveAndFlush(type)); }
        catch (DataIntegrityViolationException exception) {
            // MySQL duplicate-key errors also cover concurrent requests that passed the precheck.
            Throwable cause = exception;
            while (cause != null) {
                if (cause instanceof java.sql.SQLException sql && sql.getErrorCode() == 1062) throw duplicate();
                cause = cause.getCause();
            }
            throw exception;
        }
    }

    private DuplicateResourceException duplicate() { return new DuplicateResourceException("A leave type with this code already exists."); }

    private LeaveTypeResponse response(LeaveType type) {
        return new LeaveTypeResponse(type.getId(), type.getCode(), type.getName(), type.getDescription(),
                type.getUnit(), type.isPaid(), type.getStatus(), type.getCreatedOn(), type.getCreatedBy(),
                type.getUpdatedOn(), type.getUpdatedBy());
    }
}
