package com.rit.performance.mapper;

import com.rit.performance.dto.FinalRatingResponse;
import com.rit.performance.entity.FinalRating;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeReview;
import com.rit.performance.entity.PerformanceCycles;
import com.rit.performance.entity.User;

public final class FinalRatingMapper {

    private FinalRatingMapper() {
    }

    public static FinalRatingResponse toResponse(FinalRating entity) {
        if (entity == null) {
            return null;
        }
        EmployeeReview review = entity.getEmployeeReview();
        Employee employee = review == null ? null : review.getEmployee();
        PerformanceCycles cycle = review == null ? null : review.getPerformanceCycle();
        User publisher = entity.getPublishedBy();
        String employeeName = employee == null ? null
                : (employee.getFirstName() + " " + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
        return FinalRatingResponse.builder()
                .id(entity.getId())
                .employeeReviewId(review == null ? null : review.getId())
                .employeeId(employee == null ? null : employee.getId()).employeeName(employeeName)
                .cycleId(cycle == null ? null : cycle.getId()).cycleName(cycle == null ? null : cycle.getCycleName())
                .finalRating(entity.getFinalRating())
                .published(entity.getPublished())
                .publishedDate(entity.getPublishedDate())
                .publishedById(publisher == null ? null : publisher.getId())
                .publishedByUsername(publisher == null ? null : publisher.getUsername())
                .build();
    }
}
