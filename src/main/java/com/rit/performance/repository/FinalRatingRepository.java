package com.rit.performance.repository;

import com.rit.performance.entity.FinalRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface FinalRatingRepository extends JpaRepository<FinalRating, Long> {
    Optional<FinalRating> findByEmployeeReviewId(Long employeeReviewId);

    List<FinalRating> findByEmployeeReviewIdIn(List<Long> employeeReviewIds);

    @Query("""
            select rating from FinalRating rating
            where rating.employeeReview.employee.id = :employeeId
              and rating.employeeReview.performanceCycle.id = :cycleId
            """)
    Optional<FinalRating> findByEmployeeAndCycle(
            @Param("employeeId") Long employeeId, @Param("cycleId") Long cycleId);
}
