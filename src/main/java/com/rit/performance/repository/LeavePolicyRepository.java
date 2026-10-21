package com.rit.performance.repository;

import com.rit.performance.entity.LeavePolicy;
import com.rit.performance.entity.LeavePolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LeavePolicyRepository extends JpaRepository<LeavePolicy, Long> {
    List<LeavePolicy> findAllByOrderByPolicyNameAscIdAsc();
    List<LeavePolicy> findByStatusOrderByPolicyNameAscIdAsc(LeavePolicyStatus status);
}
