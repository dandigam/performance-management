package com.rit.performance.repository;

import com.rit.performance.entity.LeavePolicyRule;
import com.rit.performance.entity.LeavePolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LeavePolicyRuleRepository extends JpaRepository<LeavePolicyRule, Long> {
    boolean existsByLeavePolicyIdAndLeaveTypeId(Long leavePolicyId, Long leaveTypeId);
    boolean existsByLeavePolicyIdAndLeaveTypeIdAndIdNot(Long leavePolicyId, Long leaveTypeId, Long id);
    List<LeavePolicyRule> findByLeavePolicyIdOrderByIdAsc(Long leavePolicyId);
    Optional<LeavePolicyRule> findByLeavePolicyIdAndLeaveTypeIdAndStatus(
            Long leavePolicyId, Long leaveTypeId, LeavePolicyStatus status);
}
