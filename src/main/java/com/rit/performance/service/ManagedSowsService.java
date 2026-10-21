package com.rit.performance.service;

import com.rit.performance.dto.response.ManagedSowsResponse;
import com.rit.performance.dto.response.ManagedSowsResponse.ManagedSow;
import com.rit.performance.entity.Sow;
import com.rit.performance.repository.SowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagedSowsService {
    private final CurrentEmployeeService currentEmployee;
    private final SowRepository sows;

    public ManagedSowsResponse getMyManagedSows() {
        Long employeeId = currentEmployee.currentEmployee().getId();
        return new ManagedSowsResponse(sows.findManagedByEmployeeId(employeeId).stream()
                .map(sow -> toResponse(sow, employeeId)).toList());
    }

    private ManagedSow toResponse(Sow sow, Long employeeId) {
        List<String> responsibilities = new ArrayList<>(2);
        if (sow.getDeliveryOwnerEmployee() != null
                && employeeId.equals(sow.getDeliveryOwnerEmployee().getId()))
            responsibilities.add("DELIVERY_OWNER");
        if (sow.getTechnicalLeadEmployee() != null
                && employeeId.equals(sow.getTechnicalLeadEmployee().getId()))
            responsibilities.add("TECHNICAL_LEAD");
        // Existing SOW routes allow authenticated users; the JWT filter excludes
        // onboarding-only accounts from both this endpoint and SOW detail routes.
        return new ManagedSow(sow.getId(), sow.getSowName(),
                sow.getStatus() == null ? null : sow.getStatus().getCode(),
                sow.getStartDate(), sow.getEndDate(), List.copyOf(responsibilities), true);
    }
}
