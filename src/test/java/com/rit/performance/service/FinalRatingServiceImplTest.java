package com.rit.performance.service;

import com.rit.performance.dto.FinalRatingResponse;
import com.rit.performance.entity.EmployeeReview;
import com.rit.performance.entity.EmployeeReviewAssessment;
import com.rit.performance.entity.FinalRating;
import com.rit.performance.entity.User;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.EmployeeReviewRepository;
import com.rit.performance.repository.FinalRatingRepository;
import com.rit.performance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinalRatingServiceImplTest {

    @Mock private FinalRatingRepository finalRatingRepository;
    @Mock private EmployeeReviewRepository employeeReviewRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailNotificationService emailNotificationService;

    private FinalRatingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinalRatingServiceImpl(finalRatingRepository, employeeReviewRepository,
                employeeRepository, userRepository, emailNotificationService);
    }

    @Test
    void publishesSelfRatingWhenSelfIsTheOnlyConfiguredStage() {
        EmployeeReview review = submittedReview(54L, List.of(submittedAssessment(1, "5.00")));
        preparePublish(review);

        FinalRatingResponse response = service.publishRating(54L, 3L);

        assertEquals(new BigDecimal("5.00"), response.getFinalRating());
    }

    @Test
    void publishesRatingFromHighestConfiguredStage() {
        EmployeeReview review = submittedReview(55L, List.of(
                submittedAssessment(1, "3.00"),
                submittedAssessment(2, "4.00"),
                submittedAssessment(3, "4.50")));
        preparePublish(review);

        FinalRatingResponse response = service.publishRating(55L, 3L);

        assertEquals(new BigDecimal("4.50"), response.getFinalRating());
    }

    private void preparePublish(EmployeeReview review) {
        User publisher = new User();
        publisher.setId(3L);
        publisher.setUsername("admin");
        publisher.setStatus("ACTIVE");
        when(employeeReviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
        when(userRepository.findById(3L)).thenReturn(Optional.of(publisher));
        when(finalRatingRepository.findByEmployeeReviewId(review.getId())).thenReturn(Optional.empty());
        when(finalRatingRepository.save(any(FinalRating.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private EmployeeReview submittedReview(Long id, List<EmployeeReviewAssessment> assessments) {
        EmployeeReview review = EmployeeReview.builder()
                .id(id)
                .status(EmployeeReviewStatus.SUBMITTED)
                .assessments(new ArrayList<>(assessments))
                .build();
        review.getAssessments().forEach(assessment -> assessment.setEmployeeReview(review));
        return review;
    }

    private EmployeeReviewAssessment submittedAssessment(int level, String rating) {
        return EmployeeReviewAssessment.builder()
                .assessmentLevel(level)
                .status(EmployeeReviewStatus.SUBMITTED)
                .overallRating(new BigDecimal(rating))
                .build();
    }
}
