package com.rit.performance.controller;

import com.rit.performance.dto.request.*;
import com.rit.performance.dto.response.*;
import com.rit.performance.entity.LeavePolicyStatus;
import com.rit.performance.service.LeavePolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/leave-policies")
public class LeavePolicyController {
    private final LeavePolicyService service;
    @PostMapping public ResponseEntity<LeavePolicyResponse> create(@Valid @RequestBody LeavePolicyRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/leave-policies/" + result.id())).body(result);
    }
    @PutMapping("/{id}") public LeavePolicyResponse update(@PathVariable Long id, @Valid @RequestBody LeavePolicyRequest request) { return service.update(id, request); }
    @GetMapping public List<LeavePolicyResponse> getAll(@RequestParam(required = false) LeavePolicyStatus status) { return service.getAll(status); }
    @GetMapping("/{id}") public LeavePolicyResponse getById(@PathVariable Long id) { return service.getById(id); }
    @PatchMapping("/{id}/status") public LeavePolicyResponse changeStatus(@PathVariable Long id, @Valid @RequestBody LeavePolicyStatusRequest request) { return service.changePolicyStatus(id, request.status()); }
    @PostMapping("/{id}/rules") public ResponseEntity<LeavePolicyRuleResponse> addRule(@PathVariable Long id, @Valid @RequestBody LeavePolicyRuleRequest request) {
        var result = service.addRule(id, request);
        return ResponseEntity.created(URI.create("/api/v1/leave-policies/" + id + "/rules/" + result.id())).body(result);
    }
    @PutMapping("/{policyId}/rules/{ruleId}") public LeavePolicyRuleResponse updateRule(@PathVariable Long policyId, @PathVariable Long ruleId, @Valid @RequestBody LeavePolicyRuleRequest request) { return service.updateRule(policyId, ruleId, request); }
    @PatchMapping("/{policyId}/rules/{ruleId}/status") public LeavePolicyRuleResponse changeRuleStatus(@PathVariable Long policyId, @PathVariable Long ruleId, @Valid @RequestBody LeavePolicyStatusRequest request) { return service.changeRuleStatus(policyId, ruleId, request.status()); }
    @DeleteMapping("/{policyId}/rules/{ruleId}") public ResponseEntity<Void> removeRule(@PathVariable Long policyId, @PathVariable Long ruleId) { service.deactivateRule(policyId, ruleId); return ResponseEntity.noContent().build(); }
}
