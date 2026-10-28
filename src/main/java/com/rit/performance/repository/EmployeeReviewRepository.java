package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface EmployeeReviewRepository extends JpaRepository<EmployeeReview, Long> {
    Optional<EmployeeReview> findByEmployeeIdAndPerformanceCycleId(Long employeeId, Long cycleId);

    List<EmployeeReview> findByEmployeeIdOrderByCreatedOnDesc(Long employeeId);

    List<EmployeeReview> findByPerformanceCycleId(Long cycleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select review from EmployeeReview review where review.performanceCycle.id = :cycleId")
    List<EmployeeReview> findForPublication(@Param("cycleId") Long cycleId);

    List<EmployeeReview> findByPerformanceCycleIdAndEmployeeIdIn(Long cycleId, List<Long> employeeIds);

    boolean existsByPerformanceCycleIdAndEmployeeId(Long cycleId, Long employeeId);
}
