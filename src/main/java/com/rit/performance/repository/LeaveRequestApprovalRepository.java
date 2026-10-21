package com.rit.performance.repository;

import com.rit.performance.entity.LeaveRequestApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LeaveRequestApprovalRepository extends JpaRepository<LeaveRequestApproval, Long> {
    List<LeaveRequestApproval> findByLeaveRequestIdOrderByActionAtAscIdAsc(Long requestId);
}
