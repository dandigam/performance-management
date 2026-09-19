package com.rit.performance.repository;

import com.rit.performance.entity.LeaveType;
import com.rit.performance.entity.LeaveTypeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    List<LeaveType> findAllByOrderByNameAscIdAsc();
    List<LeaveType> findByStatusOrderByNameAscIdAsc(LeaveTypeStatus status);
}
