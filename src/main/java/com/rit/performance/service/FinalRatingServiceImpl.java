package com.rit.performance.service;

import com.rit.performance.dto.FinalRatingResponse;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeReview;
import com.rit.performance.entity.EmployeeReviewAssessment;
import com.rit.performance.entity.FinalRating;
import com.rit.performance.entity.User;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.mapper.FinalRatingMapper;
import com.rit.performance.repository.EmployeeReviewRepository;
import com.rit.performance.repository.FinalRatingRepository;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class FinalRatingServiceImpl implements FinalRatingService {

    private final FinalRatingRepository finalRatingRepository;
    private final EmployeeReviewRepository employeeReviewRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;

    public FinalRatingServiceImpl(
            FinalRatingRepository finalRatingRepository,
            EmployeeReviewRepository employeeReviewRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            EmailNotificationService emailNotificationService) {
        this.finalRatingRepository = finalRatingRepository;
        this.employeeReviewRepository = employeeReviewRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinalRatingResponse> getAllFinalRatings() {
        Map<Long, List<FinalRatingResponse>> ratingsByEmployee = new LinkedHashMap<>();
        finalRatingRepository.findAll().stream()
                .map(FinalRatingMapper::toResponse)
                .forEach(rating -> ratingsByEmployee
                        .computeIfAbsent(rating.getEmployeeId(), ignored -> new java.util.ArrayList<>())
                        .add(rating));

        return employeeRepository.findAll().stream()
                .sorted(Comparator.comparing(Employee::getId, Comparator.nullsLast(Long::compareTo)))
                .map(employee -> {
                    List<FinalRatingResponse> performance = ratingsByEmployee
                            .getOrDefault(employee.getId(), List.of());
                    if (performance.isEmpty()) {
                        return FinalRatingResponse.builder()
                                .employeeId(employee.getId())
                                .employeeName(employeeName(employee))
                                .performance(List.of())
                                .build();
                    }
                    return performance.get(0).toBuilder()
                            .performance(List.copyOf(performance))
                            .build();
                })
                .toList();
    }

    private static String employeeName(Employee employee) {
        return (employee.getFirstName() + " "
                + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
    }

    @Override
    @Transactional(readOnly = true)
    public FinalRatingResponse getFinalRatingById(Long id) {
        FinalRating rating = finalRatingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Final rating not found"));
        return FinalRatingMapper.toResponse(rating);
    }

    @Override
    public FinalRatingResponse publishRating(Long employeeReviewId, Long publishedById) {
        EmployeeReview review = employeeReviewRepository.findById(employeeReviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee review not found: " + employeeReviewId));
        User publisher = requireActivePublisher(publishedById);
        FinalRating existing = finalRatingRepository.findByEmployeeReviewId(employeeReviewId).orElse(null);
        if (existing != null && Boolean.TRUE.equals(existing.getPublished()))
            throw new InvalidOperationException("Final rating is already published");
        return FinalRatingMapper.toResponse(publish(review, publisher, existing));
    }

    @Override
    @Transactional(readOnly = true)
    public FinalRatingResponse getMyRating(Long employeeId, Long cycleId) {
        if (!employeeRepository.existsById(employeeId))
            throw new ResourceNotFoundException("Employee not found: " + employeeId);
        FinalRating finalRating = finalRatingRepository.findByEmployeeAndCycle(employeeId, cycleId)
                .filter(rating -> Boolean.TRUE.equals(rating.getPublished()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Published final rating not found for employee " + employeeId + " and cycle " + cycleId));

        return FinalRatingMapper.toResponse(finalRating);
    }

    private FinalRating publish(EmployeeReview review, User publisher, FinalRating existing) {
        if (review.getStatus() != EmployeeReviewStatus.SUBMITTED)
            throw new InvalidOperationException("All review assessments must be submitted before publishing");
        EmployeeReviewAssessment finalAssessment = finalAssessment(review);
        if (finalAssessment.getStatus() != EmployeeReviewStatus.SUBMITTED)
            throw new InvalidOperationException("Final assessment must be submitted before publishing");
        if (finalAssessment.getOverallRating() == null)
            throw new InvalidOperationException("Final assessment overall rating is required before publishing");
        FinalRating rating = existing == null ? new FinalRating() : existing;
        rating.setEmployeeReview(review);
        rating.setFinalRating(finalAssessment.getOverallRating());
        rating.setPublished(true);
        rating.setPublishedBy(publisher);
        rating.setPublishedDate(LocalDateTime.now());
        rating = finalRatingRepository.save(rating);
        emailNotificationService.queueResultPublished(rating);
        return rating;
    }

    private User requireActivePublisher(Long publishedById) {
        User publisher = userRepository.findById(publishedById)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher user not found: " + publishedById));
        if (!"ACTIVE".equalsIgnoreCase(publisher.getStatus()))
            throw new InvalidOperationException("Publisher user must be active");
        return publisher;
    }

    private EmployeeReviewAssessment finalAssessment(EmployeeReview review) {
        return review.getAssessments().stream()
                .max(Comparator.comparing(EmployeeReviewAssessment::getAssessmentLevel))
                .orElseThrow(() -> new InvalidOperationException(
                        "At least one assessment is required before publishing"));
    }
}
