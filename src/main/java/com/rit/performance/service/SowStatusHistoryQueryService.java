package com.rit.performance.service;

import com.rit.performance.dto.response.SowStatusHistoryResponse;
import com.rit.performance.entity.User;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SowStatusHistoryQueryService {
    private final SowRepository sows;
    private final SowStatusHistoryRepository history;
    private final UserRepository users;

    public List<SowStatusHistoryResponse> list(Long sowId) {
        if (!sows.existsById(sowId)) throw new ResourceNotFoundException("SOW not found: " + sowId);
        var rows = history.findBySowIdOrderByChangedAtAscIdAsc(sowId);
        var actorIds = rows.stream().map(r -> r.getChangedBy()).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> names = new HashMap<>();
        if (!actorIds.isEmpty()) {
            for (User user : users.findAllById(actorIds)) {
                var employee = user.getEmployee();
                String name = employee == null ? "" :
                        (Objects.toString(employee.getFirstName(), "") + " "
                                + Objects.toString(employee.getLastName(), "")).trim();
                names.put(user.getId(), name.isBlank() ? user.getUsername() : name);
            }
        }
        return rows.stream().map(r -> new SowStatusHistoryResponse(r.getId(), r.getSowId(),
                r.getPreviousStatus(), r.getStatus(), r.getStatusEffectiveDate(), r.getChangedAt(),
                r.getChangedBy(), names.get(r.getChangedBy()), r.getApprovedAt(), r.isBaseline(), r.getReason())).toList();
    }
}
