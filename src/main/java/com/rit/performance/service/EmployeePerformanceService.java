package com.rit.performance.service;

import com.rit.performance.dto.EmployeePerformanceResponse;
import com.rit.performance.dto.EmployeePerformanceResponse.ReviewSummary;
import com.rit.performance.dto.EmployeePerformanceResponse.StageRating;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeReview;
import com.rit.performance.entity.EmployeeReviewAssessment;
import com.rit.performance.entity.FinalRating;
import com.rit.performance.entity.User;
import com.rit.performance.exception.AuthenticationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.EmployeeReviewRepository;
import com.rit.performance.repository.FinalRatingRepository;
import com.rit.performance.repository.UserRepository;
import com.rit.performance.security.AppUserDetailsService;
import com.rit.performance.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Transactional(readOnly = true)
public class EmployeePerformanceService {
    private final EmployeeRepository employees;
    private final EmployeeReviewRepository reviews;
    private final FinalRatingRepository ratings;
    private final UserRepository users;
    private final AppUserDetailsService userDetails;

    public EmployeePerformanceResponse getPerformance(Long employeeId) {
        User actor = currentUser();
        Long actorEmployeeId = actor.getEmployee() == null ? null : actor.getEmployee().getId();
        boolean privileged = Set.of("ROLE_ADMIN", "ROLE_HR").contains(userDetails.roleAuthority(actor.getRole()));
        boolean self = Objects.equals(actorEmployeeId, employeeId);
        List<EmployeeReview> visibleReviews = reviews.findByEmployeeIdOrderByCreatedOnDesc(employeeId);
        if (!privileged && !self) {
            visibleReviews = visibleReviews.stream().filter(review -> assignedLevel(review, actorEmployeeId) != null).toList();
            if (visibleReviews.isEmpty()) {
                throw new AccessDeniedException("You are not authorized to view this employee's performance");
            }
        }
        Employee employee = employees.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        Map<Long, FinalRating> finalRatings = visibleReviews.isEmpty() ? Map.of()
                : ratings.findByEmployeeReviewIdIn(visibleReviews.stream().map(EmployeeReview::getId).toList()).stream()
                    .filter(rating -> Boolean.TRUE.equals(rating.getPublished()))
                    .collect(Collectors.toMap(rating -> rating.getEmployeeReview().getId(), Function.identity()));
        List<ReviewSummary> summaries = new ArrayList<>();
        for (EmployeeReview review : visibleReviews) {
            Integer level = assignedLevel(review, actorEmployeeId);
            List<StageRating> stages = review.getAssessments().stream()
                    .filter(stage -> privileged || (self
                            ? Objects.equals(actorEmployeeId, stage.getAssessorEmployee().getId())
                            : reviewerCanSee(stage, actorEmployeeId, level)))
                    .sorted(Comparator.comparing(EmployeeReviewAssessment::getAssessmentLevel))
                    .map(this::stageRating).toList();
            FinalRating published = finalRatings.get(review.getId());
            summaries.add(new ReviewSummary(review.getId(), review.getPerformanceCycle().getId(),
                    review.getPerformanceCycle().getCycleName(), review.getStatus(), stages,
                    published == null ? null : published.getFinalRating(),
                    published == null ? null : published.getPublishedDate()));
        }
        return new EmployeePerformanceResponse(employee.getId(), name(employee), summaries);
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new AuthenticationException("Authentication is required.");
        }
        User actor = users.findCurrentUser(principal.id())
                .orElseThrow(() -> new AuthenticationException("Account is not available."));
        if (!"ACTIVE".equalsIgnoreCase(actor.getStatus())) {
            throw new AuthenticationException("An active account is required.");
        }
        return actor;
    }

    private Integer assignedLevel(EmployeeReview review, Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        return review.getAssessments().stream()
                .filter(stage -> Objects.equals(employeeId, stage.getAssessorEmployee().getId()))
                .map(EmployeeReviewAssessment::getAssessmentLevel).max(Integer::compareTo).orElse(null);
    }

    private boolean reviewerCanSee(EmployeeReviewAssessment stage, Long employeeId, Integer level) {
        return level != null && (stage.getAssessmentLevel() < level && stage.getStatus() == EmployeeReviewStatus.SUBMITTED
                || Objects.equals(stage.getAssessmentLevel(), level)
                    && Objects.equals(employeeId, stage.getAssessorEmployee().getId()));
    }

    private StageRating stageRating(EmployeeReviewAssessment stage) {
        return new StageRating(stage.getId(), stage.getAssessmentLevel(),
                AssessmentAssigneeResolver.stageCode(stage.getAssessorRole()), stage.getAssessorRole().getName(),
                stage.getAssessorEmployee().getId(), name(stage.getAssessorEmployee()), stage.getStatus(),
                stage.getOverallRating(), stage.getOverallComment(), stage.getSubmittedDate());
    }

    private String name(Employee employee) {
        return (Objects.toString(employee.getFirstName(), "") + " " + Objects.toString(employee.getLastName(), "")).trim();
    }
}
