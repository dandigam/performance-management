package com.rit.performance.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeBasicInfoResponseJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void excludesAssignmentHistoryAndIncludesCurrentProjects() {
        EmployeeAssignmentResponse assignment = EmployeeAssignmentResponse.builder()
                .departmentId(48L)
                .departmentName("Car Management")
                .build();
        EmployeeBasicInfoResponse response = EmployeeBasicInfoResponse.builder()
                .employeeId(3L)
                .designationId(42L)
                .designationName("Chief Technology Officer")
                .assignmentList(List.of(assignment))
                .currentProjects(List.of(EmployeeCurrentProjectResponse.builder()
                        .projectId(15L)
                        .projectName("Performance Management System")
                        .sowId(19L)
                        .sowName("Sow")
                        .designationName("Java Developer")
                        .build()))
                .build();

        JsonNode json = objectMapper.valueToTree(response);

        List<String> assignmentFields = List.of(
                "assignmentId", "sowId", "sowCode", "sowName", "milestoneId", "milestoneName",
                "positionType", "isPrimaryAssignment", "allocationPercentage",
                "assignmentStartDate", "assignmentEndDate", "assignmentStatus",
                "departmentId", "departmentName", "managerId", "managerName",
                "leadId", "leadName");

        assertThat(assignmentFields).allMatch(field -> !json.has(field));
        assertThat(json.has("assignmentList")).isFalse();
        assertThat(json.get("designationId").asLong()).isEqualTo(42L);
        assertThat(json.get("designationName").asText()).isEqualTo("Chief Technology Officer");
        assertThat(json.at("/currentProjects/0/projectId").asLong()).isEqualTo(15L);
        assertThat(json.at("/currentProjects/0/projectName").asText())
                .isEqualTo("Performance Management System");
        assertThat(json.at("/currentProjects/0/sowId").asLong()).isEqualTo(19L);
        assertThat(json.at("/currentProjects/0/sowName").asText()).isEqualTo("Sow");
        assertThat(json.at("/currentProjects/0/designationName").asText())
                .isEqualTo("Java Developer");
    }
}
