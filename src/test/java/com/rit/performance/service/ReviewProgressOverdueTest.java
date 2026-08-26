package com.rit.performance.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewProgressOverdueTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 2);

    @Test
    void marksPastDueIncompleteAssessmentOverdueWithoutChangingStatus() {
        assertTrue(EmployeeReviewServiceImpl.calculateOverdue(
                LocalDate.of(2026, 8, 1), EmployeeReviewStatus.IN_PROGRESS, TODAY));
        assertTrue(EmployeeReviewServiceImpl.calculateOverdue(
                LocalDate.of(2026, 8, 1), EmployeeReviewStatus.NOT_STARTED, TODAY));
    }

    @Test
    void submittedOrNotYetDueAssessmentIsNotOverdue() {
        assertFalse(EmployeeReviewServiceImpl.calculateOverdue(
                LocalDate.of(2026, 8, 1), EmployeeReviewStatus.SUBMITTED, TODAY));
        assertFalse(EmployeeReviewServiceImpl.calculateOverdue(
                TODAY, EmployeeReviewStatus.IN_PROGRESS, TODAY));
        assertFalse(EmployeeReviewServiceImpl.calculateOverdue(
                null, EmployeeReviewStatus.IN_PROGRESS, TODAY));
    }
}
