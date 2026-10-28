package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeReviewAssessment;
import com.rit.performance.service.EmployeeReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeReviewAssessmentRepository extends JpaRepository<EmployeeReviewAssessment, Long> {
    @Query("""
            select a from EmployeeReviewAssessment a
            join fetch a.employeeReview r
            join fetch r.employee e
            join fetch a.assessorRole
            where a.assessorEmployee.id = :reviewerEmployeeId
              and r.performanceCycle.id = :cycleId
            order by lower(e.firstName), lower(e.lastName), e.id, a.assessmentLevel, a.id
            """)
    List<EmployeeReviewAssessment> findAssignedTeamAssessments(
            @Param("reviewerEmployeeId") Long reviewerEmployeeId, @Param("cycleId") Long cycleId);

    List<EmployeeReviewAssessment> findByEmployeeReviewIdOrderByAssessmentLevelAsc(Long employeeReviewId);
    List<EmployeeReviewAssessment> findByEmployeeReviewIdAndAssessorEmployeeIdOrderByAssessmentLevelAsc(
            Long reviewId, Long assessorEmployeeId);
    boolean existsByEmployeeReviewIdAndAssessmentLevel(Long reviewId, Integer assessmentLevel);
    boolean existsByEmployeeReviewIdAndAssessorEmployeeIdAndStatus(
            Long reviewId, Long assessorEmployeeId, EmployeeReviewStatus status);

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
