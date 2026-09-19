package com.rit.performance.service;

import com.rit.performance.dto.request.LeavePolicyRequest;
import com.rit.performance.dto.request.LeavePolicyRuleRequest;
import com.rit.performance.entity.*;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LeavePolicyServiceTest {
    private final LeavePolicyRepository policies = mock(LeavePolicyRepository.class);
    private final LeavePolicyRuleRepository rules = mock(LeavePolicyRuleRepository.class);
    private final LeaveTypeRepository types = mock(LeaveTypeRepository.class);
    private final LeavePolicyService service = new LeavePolicyService(policies, rules, types);

    private LeavePolicyRequest policyRequest(LocalDate end) {
        return new LeavePolicyRequest(" US full time ", LeavePolicyCountry.USA, EmploymentType.FULL_TIME,
                LocalDate.of(2026, 1, 1), end, null);
    }

    private LeavePolicyRuleRequest ruleRequest(Long typeId) {
        return new LeavePolicyRuleRequest(typeId, new BigDecimal("20.00"), true,
                new BigDecimal("5.00"), null);
    }

    @Test void createsPolicyAndRejectsInvalidDates() {
        when(policies.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var created = service.create(policyRequest(null));
        assertEquals("US full time", created.policyName());
        assertEquals(LeavePolicyStatus.ACTIVE, created.status());
        assertEquals(List.of(), created.rules());
        assertThrows(InvalidOperationException.class,
                () -> service.create(policyRequest(LocalDate.of(2025, 12, 31))));
    }

    @Test void addsRuleAndPreventsDuplicateEvenWhenInactive() {
        var policy = new LeavePolicy(); policy.setId(1L);
        var type = new LeaveType(); type.setId(2L); type.setCode("ANNUAL"); type.setName("Annual");
        when(policies.findById(1L)).thenReturn(Optional.of(policy));
        when(types.findById(2L)).thenReturn(Optional.of(type));
        when(rules.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var added = service.addRule(1L, ruleRequest(2L));
        assertEquals(2L, added.leaveTypeId());
        assertEquals(new BigDecimal("5.00"), added.maxCarryForward());
        when(rules.existsByLeavePolicyIdAndLeaveTypeId(1L, 2L)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.addRule(1L, ruleRequest(2L)));
    }

    @Test void deactivatesRuleWithoutDeletingItAndChecksPolicyOwnership() {
        var policy = new LeavePolicy(); policy.setId(1L);
        var type = new LeaveType(); type.setId(2L);
        var rule = new LeavePolicyRule(); rule.setId(3L); rule.setLeavePolicy(policy); rule.setLeaveType(type);
        when(rules.findById(3L)).thenReturn(Optional.of(rule));
        service.deactivateRule(1L, 3L);
        assertEquals(LeavePolicyStatus.INACTIVE, rule.getStatus());
        verify(rules).saveAndFlush(rule);
        verify(rules, never()).delete(any());
        assertThrows(ResourceNotFoundException.class, () -> service.deactivateRule(9L, 3L));
    }
}
