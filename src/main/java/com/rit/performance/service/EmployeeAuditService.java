package com.rit.performance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rit.performance.dto.EmployeeAuditHistoryResponse;
import com.rit.performance.entity.EmployeeAuditHistory;
import com.rit.performance.entity.User;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.EmployeeAuditHistoryRepository;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeAuditService {
    private final EmployeeAuditHistoryRepository repository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public void record(Long employeeId, String entityType, String action,
                       Object oldValues, Object newValues, Long changedBy) {
        repository.save(EmployeeAuditHistory.builder()
                .employeeId(employeeId)
                .entityType(entityType)
                .action(action)
                .oldValues(json(oldValues))
                .newValues(json(newValues))
                .changedBy(changedBy)
                .changedOn(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public List<EmployeeAuditHistoryResponse> getHistory(Long employeeId) {
        if (!employeeRepository.existsById(employeeId))
            throw new ResourceNotFoundException("Employee not found: " + employeeId);
        return repository.findByEmployeeIdOrderByChangedOnDescIdDesc(employeeId).stream()
                .map(this::response).toList();
    }

    private EmployeeAuditHistoryResponse response(EmployeeAuditHistory history) {
        User actor = history.getChangedBy() == null ? null
                : userRepository.findById(history.getChangedBy()).orElse(null);
        String name = actor == null || actor.getEmployee() == null ? null
                : ((actor.getEmployee().getFirstName() + " "
                + (actor.getEmployee().getLastName() == null ? "" : actor.getEmployee().getLastName())).trim());
        return EmployeeAuditHistoryResponse.builder()
                .id(history.getId()).employeeId(history.getEmployeeId())
                .entityType(history.getEntityType()).action(history.getAction())
                .changedBy(history.getChangedBy()).changedByName(name)
                .changedByEmail(actor == null ? null : actor.getUsername())
                .changedOn(history.getChangedOn())
                .oldValues(tree(history.getOldValues())).newValues(tree(history.getNewValues()))
                .build();
    }

    private String json(Object value) {
        if (value == null) return null;
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) {
            throw new InvalidOperationException("Unable to create employee audit snapshot");
        }
    }

    private JsonNode tree(String value) {
        if (value == null) return null;
        try { return objectMapper.readTree(value); }
        catch (JsonProcessingException exception) { return objectMapper.createObjectNode(); }
    }
}
