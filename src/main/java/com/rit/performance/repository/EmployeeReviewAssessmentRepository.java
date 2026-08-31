package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeReviewAssessment;
import com.rit.performance.service.EmployeeReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeReviewAssessmentRepository extends JpaRepository<EmployeeReviewAssessment, Long> {
    List<EmployeeReviewAssessment> findByEmployeeReviewIdOrderByAssessmentLevelAsc(Long employeeReviewId);
    List<EmployeeReviewAssessment> findByEmployeeReviewIdAndAssessorEmployeeIdOrderByAssessmentLevelAsc(
            Long reviewId, Long assessorEmployeeId);
    boolean existsByEmployeeReviewIdAndAssessmentLevel(Long reviewId, Integer assessmentLevel);
    boolean existsByEmployeeReviewIdAndAssessorEmployeeIdAndStatus(
            Long reviewId, Long assessorEmployeeId, EmployeeReviewStatus status);

    List<EmployeeReviewAssessment> findByEmployeeReviewIdInOrderByEmployeeReviewIdAscAssessmentLevelAsc(
            List<Long> reviewIds);

    @Query("""
            select assessment
            from EmployeeReviewAssessment assessment
            where assessment.assessorEmployee.id = :assessorEmployeeId
              and assessment.employeeReview.performanceCycle.id = :cycleId
              and assessment.employeeReview.employee.id <> :assessorEmployeeId
            order by assessment.employeeReview.employee.id, assessment.assessmentLevel
            """)
    List<EmployeeReviewAssessment> findAssignedReviewsForAssessor(
            @Param("assessorEmployeeId") Long assessorEmployeeId, @Param("cycleId") Long cycleId);

    @Query("""
            select assessment
            from EmployeeReviewAssessment assessment
            where assessment.employeeReview.employee.id in :employeeIds
              and assessment.assessorEmployee.id = assessment.employeeReview.employee.id
            order by assessment.updatedOn desc, assessment.id desc
            """)
    List<EmployeeReviewAssessment> findSelfAssessmentsForEmployeesOrderByUpdatedOnDesc(
            @Param("employeeIds") List<Long> employeeIds);
}
