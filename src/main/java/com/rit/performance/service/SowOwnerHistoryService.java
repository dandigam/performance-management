package com.rit.performance.service;

import com.rit.performance.entity.*;
import com.rit.performance.dto.response.SowOwnerHistoryResponse;
import com.rit.performance.exception.*;
import com.rit.performance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class SowOwnerHistoryService {
    private final SowOwnerHistoryRepository history;
    private final SowRepository sows;
    private final AuditorAware<Long> auditor;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Sow sow, Employee previousDeliveryOwner, Employee previousTechnicalLead,
                       LocalDate effectiveDate, String reason) {
        LocalDate date = effectiveDate == null ? LocalDate.now() : effectiveDate;
        if (date.isAfter(LocalDate.now())) throw new InvalidOperationException("Owner effective date cannot be in the future");
        if (reason != null && reason.length() > 2000) throw new InvalidOperationException("Owner change reason must not exceed 2000 characters");
        recordRole(sow.getId(), "DELIVERY_OWNER", previousDeliveryOwner, sow.getDeliveryOwnerEmployee(), date, reason);
        recordRole(sow.getId(), "TECHNICAL_LEAD", previousTechnicalLead, sow.getTechnicalLeadEmployee(), date, reason);
    }

    private void recordRole(Long sowId, String role, Employee previous, Employee current, LocalDate date, String reason) {
        if (Objects.equals(id(previous), id(current))) return;
        if (current != null && !"ACTIVE".equalsIgnoreCase(current.getStatus()))
            throw new InvalidOperationException("New " + role + " must be an active employee");
        // Hibernate-only deployments also capture the pre-existing owner on the first change.
        if (previous != null && !history.existsBySowIdAndRole(sowId, role))
            append(sowId, role, null, previous, null, "Existing ownership snapshot; original effective date unknown", true);
        append(sowId, role, previous, current, date, reason, false);
    }

    private void append(Long sowId, String role, Employee previous, Employee current,
                        LocalDate date, String reason, boolean baseline) {
        var row = new SowOwnerHistory();
        row.setSowId(sowId); row.setRole(role);
        row.setPreviousEmployeeId(id(previous)); row.setPreviousEmployeeName(name(previous));
        row.setEmployeeId(id(current)); row.setEmployeeName(name(current));
        row.setEffectiveDate(date); row.setReason(reason == null || reason.isBlank() ? null : reason.trim());
        row.setChangedAt(LocalDateTime.now()); row.setChangedBy(baseline ? null : auditor.getCurrentAuditor().orElse(null));
        row.setBaseline(baseline);
        history.save(row);
    }

    public List<SowOwnerHistoryResponse> list(Long sowId) {
        if (!sows.existsById(sowId)) throw new ResourceNotFoundException("SOW not found: " + sowId);
        return history.findBySowIdOrderByChangedAtAscIdAsc(sowId).stream().map(r -> new SowOwnerHistoryResponse(
                r.getId(), r.getSowId(), r.getRole(), r.getPreviousEmployeeId(), r.getPreviousEmployeeName(),
                r.getEmployeeId(), r.getEmployeeName(), r.getEffectiveDate(), r.getReason(),
                r.getChangedAt(), r.getChangedBy(), r.isBaseline())).toList();
    }
    private Long id(Employee e) { return e == null ? null : e.getId(); }
    private String name(Employee e) {
        return e == null ? null : (Objects.toString(e.getFirstName(), "") + " " + Objects.toString(e.getLastName(), "")).trim();
    }
}
