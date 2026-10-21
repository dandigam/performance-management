package com.rit.performance.service;

import com.rit.performance.dto.request.*;
import com.rit.performance.dto.response.*;
import com.rit.performance.entity.*;
import com.rit.performance.exception.*;
import com.rit.performance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeavePolicyService {
    private final LeavePolicyRepository policyRepository;
    private final LeavePolicyRuleRepository ruleRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    @Transactional
    public LeavePolicyResponse create(LeavePolicyRequest request) {
        LeavePolicy policy = new LeavePolicy();
        apply(policy, request);
        if (request.status() != null) policy.setStatus(request.status());
        return policyResponse(policyRepository.saveAndFlush(policy), List.of());
    }

    @Transactional
    public LeavePolicyResponse update(Long id, LeavePolicyRequest request) {
        LeavePolicy policy = findPolicy(id);
        apply(policy, request);
        if (request.status() != null) policy.setStatus(request.status());
        return detailResponse(policyRepository.saveAndFlush(policy));
    }

    public List<LeavePolicyResponse> getAll(LeavePolicyStatus status) {
        List<LeavePolicy> policies = status == null ? policyRepository.findAllByOrderByPolicyNameAscIdAsc()
                : policyRepository.findByStatusOrderByPolicyNameAscIdAsc(status);
        return policies.stream().map(policy -> policyResponse(policy, List.of())).toList();
    }

    public LeavePolicyResponse getById(Long id) { return detailResponse(findPolicy(id)); }

    @Transactional
    public LeavePolicyResponse changePolicyStatus(Long id, LeavePolicyStatus status) {
        LeavePolicy policy = findPolicy(id);
        policy.setStatus(status);
        return detailResponse(policyRepository.saveAndFlush(policy));
    }

    @Transactional
    public LeavePolicyRuleResponse addRule(Long policyId, LeavePolicyRuleRequest request) {
        LeavePolicy policy = findPolicy(policyId);
        if (ruleRepository.existsByLeavePolicyIdAndLeaveTypeId(policyId, request.leaveTypeId())) throw duplicateRule();
        LeavePolicyRule rule = new LeavePolicyRule();
        rule.setLeavePolicy(policy);
        rule.setLeaveType(findLeaveType(request.leaveTypeId()));
        apply(rule, request);
        if (request.status() != null) rule.setStatus(request.status());
        return persistRule(rule);
    }

    @Transactional
    public LeavePolicyRuleResponse updateRule(Long policyId, Long ruleId, LeavePolicyRuleRequest request) {
        LeavePolicyRule rule = findRule(policyId, ruleId);
        if (ruleRepository.existsByLeavePolicyIdAndLeaveTypeIdAndIdNot(policyId, request.leaveTypeId(), ruleId))
            throw duplicateRule();
        rule.setLeaveType(findLeaveType(request.leaveTypeId()));
        apply(rule, request);
        if (request.status() != null) rule.setStatus(request.status());
        return persistRule(rule);
    }

    @Transactional
    public LeavePolicyRuleResponse changeRuleStatus(Long policyId, Long ruleId, LeavePolicyStatus status) {
        LeavePolicyRule rule = findRule(policyId, ruleId);
        rule.setStatus(status);
        return persistRule(rule);
    }

    @Transactional
    public void deactivateRule(Long policyId, Long ruleId) {
        LeavePolicyRule rule = findRule(policyId, ruleId);
        rule.setStatus(LeavePolicyStatus.INACTIVE);
        ruleRepository.saveAndFlush(rule);
    }

    private LeavePolicy findPolicy(Long id) {
        return policyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave policy not found: " + id));
    }
    private LeaveType findLeaveType(Long id) {
        return leaveTypeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave type not found: " + id));
    }
    private LeavePolicyRule findRule(Long policyId, Long ruleId) {
        LeavePolicyRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave policy rule not found: " + ruleId));
        if (!rule.getLeavePolicy().getId().equals(policyId)) throw new ResourceNotFoundException("Leave policy rule not found: " + ruleId);
        return rule;
    }
    private void apply(LeavePolicy policy, LeavePolicyRequest request) {
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom()))
            throw new InvalidOperationException("effectiveTo must be on or after effectiveFrom.");
        policy.setPolicyName(request.policyName().trim());
        policy.setCountry(request.country()); policy.setEmploymentType(request.employmentType());
        policy.setEffectiveFrom(request.effectiveFrom()); policy.setEffectiveTo(request.effectiveTo());
    }
    private void apply(LeavePolicyRule rule, LeavePolicyRuleRequest request) {
        rule.setEntitlement(request.entitlement()); rule.setCarryForward(request.carryForward());
        rule.setMaxCarryForward(request.carryForward() ? request.maxCarryForward() : null);
    }
    private LeavePolicyRuleResponse persistRule(LeavePolicyRule rule) {
        try { return ruleResponse(ruleRepository.saveAndFlush(rule)); }
        catch (DataIntegrityViolationException e) {
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof java.sql.SQLException sql && sql.getErrorCode() == 1062) throw duplicateRule();
                cause = cause.getCause();
            }
            throw e;
        }
    }
    private LeavePolicyResponse detailResponse(LeavePolicy policy) {
        return policyResponse(policy, ruleRepository.findByLeavePolicyIdOrderByIdAsc(policy.getId()).stream().map(this::ruleResponse).toList());
    }
    private LeavePolicyResponse policyResponse(LeavePolicy p, List<LeavePolicyRuleResponse> rules) {
        return new LeavePolicyResponse(p.getId(), p.getPolicyName(), p.getCountry(), p.getEmploymentType(), p.getEffectiveFrom(),
                p.getEffectiveTo(), p.getStatus(), p.getCreatedOn(), p.getCreatedBy(), p.getUpdatedOn(), p.getUpdatedBy(), rules);
    }
    private LeavePolicyRuleResponse ruleResponse(LeavePolicyRule r) {
        LeaveType t = r.getLeaveType();
        return new LeavePolicyRuleResponse(r.getId(), t.getId(), t.getCode(), t.getName(), r.getEntitlement(), r.isCarryForward(),
                r.getMaxCarryForward(), r.getStatus(), r.getCreatedOn(), r.getCreatedBy(), r.getUpdatedOn(), r.getUpdatedBy());
    }
    private DuplicateResourceException duplicateRule() { return new DuplicateResourceException("This leave type has already been added to the leave policy."); }
}
