package com.rit.performance.service;

import com.rit.performance.entity.Sow;
import com.rit.performance.entity.SowStatusHistory;
import com.rit.performance.repository.SowStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SowStatusHistoryService {
    private final SowStatusHistoryRepository repository;
    private final AuditorAware<Long> auditorAware;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Sow sow, String previousStatus) {
        String status = sow.getStatus().getCode();
        if (status.equalsIgnoreCase(previousStatus)) return;
        var history = new SowStatusHistory();
        history.setSowId(sow.getId());
        history.setPreviousStatus(previousStatus);
        history.setStatus(status);
        history.setStatusEffectiveDate(sow.getStatusEffectiveDate());
        history.setChangedAt(LocalDateTime.now());
        history.setChangedBy(auditorAware.getCurrentAuditor().orElse(null));
        if ("APPROVED".equalsIgnoreCase(status)) history.setApprovedAt(history.getChangedAt());
        repository.save(history);
    }
}
