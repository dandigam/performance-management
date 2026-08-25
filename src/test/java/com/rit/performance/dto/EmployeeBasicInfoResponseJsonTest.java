package com.rit.performance.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeBasicInfoResponseJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void excludesAssignmentSummaryFromHeaderAndKeepsItOnAssignments() {
        EmployeeAssignmentResponse assignment = EmployeeAssignmentResponse.builder()
                .departmentId(48L)
                .departmentName("Car Management")
                .build();
        EmployeeBasicInfoResponse response = EmployeeBasicInfoResponse.builder()
                .employeeId(3L)
                .designationId(42L)
                .designationName("Chief Technology Officer")
                .assignmentList(List.of(assignment))
                .build();

        JsonNode json = objectMapper.valueToTree(response);

        List<String> assignmentFields = List.of(
                "assignmentId", "sowId", "sowCode", "sowName", "milestoneId", "milestoneName",
                "positionType", "isPrimaryAssignment", "allocationPercentage",
                "assignmentStartDate", "assignmentEndDate", "assignmentStatus",
                "departmentId", "departmentName", "managerId", "managerName",
                "leadId", "leadName");

        assertThat(assignmentFields).allMatch(field -> !json.has(field));
        assertThat(assignmentFields).allMatch(field -> json.at("/assignmentList/0").has(field));
        assertThat(json.get("designationId").asLong()).isEqualTo(42L);
        assertThat(json.get("designationName").asText()).isEqualTo("Chief Technology Officer");
        assertThat(json.at("/assignmentList/0").has("designationId")).isTrue();
        assertThat(json.at("/assignmentList/0").has("designationName")).isTrue();
        assertThat(json.at("/assignmentList/0/departmentId").asLong()).isEqualTo(48L);
        assertThat(json.at("/assignmentList/0/departmentName").asText()).isEqualTo("Car Management");
    }
}
