package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface EmployeeReviewRepository extends JpaRepository<EmployeeReview, Long> {
    Optional<EmployeeReview> findByEmployeeIdAndPerformanceCycleId(Long employeeId, Long cycleId);

    List<EmployeeReview> findByEmployeeIdOrderByCreatedDateDesc(Long employeeId);

    List<EmployeeReview> findByPerformanceCycleId(Long cycleId);

    List<EmployeeReview> findByPerformanceCycleIdAndEmployeeIdIn(Long cycleId, List<Long> employeeIds);

    boolean existsByPerformanceCycleIdAndEmployeeId(Long cycleId, Long employeeId);
}
