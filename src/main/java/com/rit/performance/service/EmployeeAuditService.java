package com.rit.performance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class EmployeeAuditService {
    private final EmployeeAuditHistoryRepository repository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public void record(Long employeeId, String entityType, String action,
                       Object oldValues, Object newValues, Long changedBy) {
        if ("UPDATED".equalsIgnoreCase(action) && oldValues != null && newValues != null) {
            AuditDiff diff = changedFields(oldValues, newValues);
            oldValues = diff.oldValues();
            newValues = diff.newValues();
        }
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

    private AuditDiff changedFields(Object oldValues, Object newValues) {
        JsonNode oldNode = objectMapper.valueToTree(oldValues);
        JsonNode newNode = objectMapper.valueToTree(newValues);
        if (!oldNode.isObject() || !newNode.isObject()) {
            return new AuditDiff(oldValues, newValues);
        }

        ObjectNode oldChanges = objectMapper.createObjectNode();
        ObjectNode newChanges = objectMapper.createObjectNode();
        collectChanges(oldNode, newNode, oldChanges, newChanges);
        return new AuditDiff(
                objectMapper.convertValue(oldChanges, Object.class),
                objectMapper.convertValue(newChanges, Object.class));
    }

    private void collectChanges(JsonNode oldNode, JsonNode newNode,
                                ObjectNode oldChanges, ObjectNode newChanges) {
        Set<String> fields = new TreeSet<>();
        oldNode.fieldNames().forEachRemaining(fields::add);
        newNode.fieldNames().forEachRemaining(fields::add);

        for (String field : fields) {
            JsonNode oldValue = oldNode.get(field);
            JsonNode newValue = newNode.get(field);
            if (oldValue != null && newValue != null
                    && oldValue.isObject() && newValue.isObject()) {
                ObjectNode nestedOld = objectMapper.createObjectNode();
                ObjectNode nestedNew = objectMapper.createObjectNode();
                collectChanges(oldValue, newValue, nestedOld, nestedNew);
                if (!nestedOld.isEmpty()) {
                    oldChanges.set(field, nestedOld);
                    newChanges.set(field, nestedNew);
                }
            } else if (!java.util.Objects.equals(oldValue, newValue)) {
                oldChanges.set(field, oldValue == null ? objectMapper.nullNode() : oldValue);
                newChanges.set(field, newValue == null ? objectMapper.nullNode() : newValue);
            }
        }
    }

    private record AuditDiff(Object oldValues, Object newValues) {}

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
        Object oldValues = tree(history.getOldValues());
        Object newValues = tree(history.getNewValues());
        if ("UPDATED".equalsIgnoreCase(history.getAction())
                && oldValues != null && newValues != null) {
            AuditDiff diff = changedFields(oldValues, newValues);
            oldValues = diff.oldValues();
            newValues = diff.newValues();
        }
        return EmployeeAuditHistoryResponse.builder()
                .id(history.getId()).employeeId(history.getEmployeeId())
                .entityType(history.getEntityType()).action(history.getAction())
                .changedBy(history.getChangedBy()).changedByName(name)
                .changedByEmail(actor == null ? null : actor.getUsername())
                .changedOn(history.getChangedOn())
                .oldValues(oldValues).newValues(newValues)
                .build();
    }

    private String json(Object value) {
        if (value == null) return null;
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) {
            throw new InvalidOperationException("Unable to create employee audit snapshot");
        }
    }

    private Object tree(String value) {
        if (value == null) return null;
        try { return objectMapper.readValue(value, Object.class); }
        catch (JsonProcessingException exception) { return Map.of(); }
    }
}
