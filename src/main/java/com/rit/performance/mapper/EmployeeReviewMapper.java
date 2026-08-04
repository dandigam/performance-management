package com.rit.performance.mapper;

import com.rit.performance.dto.*;
import com.rit.performance.entity.*;
import com.rit.performance.service.EmployeeReviewStatus;

import java.math.BigDecimal;
import java.util.List;

public final class EmployeeReviewMapper {
    private EmployeeReviewMapper() { }

    public static EmployeeReviewResponse toResponse(EmployeeReview entity) {
        return toResponse(entity, entity == null || entity.getAssessments() == null
                ? List.of() : entity.getAssessments());
    }

    public static EmployeeReviewResponse toResponse(EmployeeReview entity,
            List<EmployeeReviewAssessment> assessments) {
        if (entity == null) return null;
        return EmployeeReviewResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployee() == null ? null : entity.getEmployee().getId())
                .cycleId(entity.getPerformanceCycle() == null ? null : entity.getPerformanceCycle().getId())
                .status(entity.getStatus())
                .progressPercentage(entity.getProgressPercentage())
                .createdBy(entity.getCreatedBy()).createdDate(entity.getCreatedDate())
                .updatedBy(entity.getUpdatedBy()).updatedDate(entity.getUpdatedDate())
                .assessmentStages(assessments == null ? List.of() : assessments.stream()
                        .map(EmployeeReviewMapper::toAssessmentResponse).toList())
                .build();
    }

    public static EmployeeReviewResponse toEmployeeResponse(EmployeeReview entity, Long employeeId) {
        if (entity == null) return null;
        List<EmployeeReviewAssessment> assessments = entity.getAssessments() == null ? List.of()
                : entity.getAssessments().stream()
                    .filter(assessment -> assessment.getAssessorEmployee() != null
                            && employeeId.equals(assessment.getAssessorEmployee().getId()))
                    .toList();
        return toResponse(entity, assessments);
    }

    public static EmployeeReviewAssessmentResponse toAssessmentResponse(EmployeeReviewAssessment entity) {
        Employee assessor = entity.getAssessorEmployee();
        String assessorName = assessor == null ? null
                : (assessor.getFirstName() + " " + (assessor.getLastName() == null ? "" : assessor.getLastName())).trim();
        BigDecimal progressPercentage = entity.getStatus() == EmployeeReviewStatus.SUBMITTED
                ? new BigDecimal("100.00") : entity.getProgressPercentage();
        return EmployeeReviewAssessmentResponse.builder()
                .id(entity.getId()).assessmentLevel(entity.getAssessmentLevel())
                .assessorRoleId(entity.getAssessorRole() == null ? null : entity.getAssessorRole().getId())
                .assessorRoleName(entity.getAssessorRole() == null ? null : entity.getAssessorRole().getName())
                .assessorEmployeeId(assessor == null ? null : assessor.getId()).assessorEmployeeName(assessorName)
                .status(entity.getStatus()).progressPercentage(progressPercentage)
                .overallRating(entity.getOverallRating())
                .overallComment(entity.getOverallComment()).startedDate(entity.getStartedDate())
                .submittedDate(entity.getSubmittedDate()).createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate()).updatedBy(entity.getUpdatedBy()).updatedDate(entity.getUpdatedDate())
                .answers(entity.getAnswers() == null ? List.of() : entity.getAnswers().stream()
                        .map(EmployeeReviewMapper::toAnswerResponse).toList())
                .build();
    }

    public static EmployeeReviewAnswerResponse toAnswerResponse(EmployeeReviewAnswer entity) {
        PerformanceCycleSection section = entity.getPerformanceCycleSection();
        PerformanceCycleQuestion question = entity.getPerformanceCycleQuestion();
        return EmployeeReviewAnswerResponse.builder()
                .id(entity.getId()).sectionId(section == null ? null : section.getId())
                .sectionName(section == null ? null : section.getSectionName())
                .questionId(question == null ? null : question.getId())
                .questionText(question == null ? null : question.getQuestionText())
                .responseType(question == null ? null : question.getResponseType())
                .required(question == null ? null : question.getRequired())
                .rating(entity.getRating()).comment(entity.getComment())
                .createdBy(entity.getCreatedBy()).createdDate(entity.getCreatedDate())
                .updatedBy(entity.getUpdatedBy()).updatedDate(entity.getUpdatedDate()).build();
    }

    public static EmployeeReviewAnswer toAnswerEntity(EmployeeReviewAssessment assessment,
                                                       EmployeeReviewAnswerRequest request) {
        return EmployeeReviewAnswer.builder().employeeReviewAssessment(assessment)
                .rating(request.getRating()).comment(request.getComment())
                .createdBy(request.getCreatedBy()).updatedBy(request.getUpdatedBy()).build();
    }
}
