package com.rit.performance.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OnboardingResponse(Long employeeId, String employeeNumber, String employeeName,
        String email, String designationName, LocalDate joiningDate, String status,
        List<String> completedSections, List<String> requiredSections, String invitationStatus,
        Instant invitationExpiresAt, Instant submittedAt, long version,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        java.util.Map<String, Object> details,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        String phoneNumber,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        String workMode,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        List<java.util.Map<String, Object>> documentTypes,
        Review review, String employmentType, String roleName, String workLocation) {
    @com.fasterxml.jackson.annotation.JsonProperty("reviewComments")
    public String reviewComments() {
        return review == null ? null : review.comments();
    }
    public record Review(String comments, String reviewedBy, Instant reviewedAt) {}
    public OnboardingResponse(Long employeeId, String employeeNumber, String employeeName,
            String email, String designationName, LocalDate joiningDate, String status,
            List<String> completedSections, List<String> requiredSections, String invitationStatus,
            Instant invitationExpiresAt, Instant submittedAt, long version) {
        this(employeeId, employeeNumber, employeeName, email, designationName, joiningDate, status,
                completedSections, requiredSections, invitationStatus, invitationExpiresAt,
                submittedAt, version, null, null, null, null, null, null, null, null);
    }
}
