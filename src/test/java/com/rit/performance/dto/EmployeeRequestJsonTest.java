package com.rit.performance.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

class EmployeeRequestJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void readsNestedProjectAssignment() throws Exception {
        EmployeeCreateRequest request = objectMapper.readValue("""
                {
                  "firstName": "John",
                  "lastName": "Doe",
                  "email": "john.doe@rit.com",
                  "roleId": 29,
                  "designationId": 19,
                  "projectAssignment": {
                    "departmentId": 48,
                    "projectId": 3,
                    "leadId": 1,
                    "managerId": 5,
                    "effectiveFrom": "2026-07-15"
                  }
                }
                """, EmployeeCreateRequest.class);

        assertNotNull(request.getProjectAssignment());
        assertEquals(1L, request.getProjectAssignment().getLeadId());
        assertEquals(5L, request.getProjectAssignment().getManagerId());
        assertTrue(request.getProjectAssignment().isLeadIdPresent());
        assertTrue(request.getProjectAssignment().isManagerIdPresent());
    }

    @Test
    void readsFlatEmployeeUpdatePayloadAliases() throws Exception {
        EmployeeUpdateRequest request = objectMapper.readValue("""
                {
                  "firstName": "Venkatesh",
                  "ritEmployeeId": "RIT100",
                  "projectId": 1,
                  "leadId": 2,
                  "managerId": 5
                }
                """, EmployeeUpdateRequest.class);

        assertEquals("RIT100", request.getRitId());
        assertEquals(1L, request.getProjectId());
        assertEquals(2L, request.getLeadId());
        assertEquals(5L, request.getManagerId());
        assertTrue(request.isProjectIdPresent());
        assertTrue(request.isLeadIdPresent());
        assertTrue(request.isManagerIdPresent());
    }

    @Test
    void readsEmployeeEmploymentAndVendorFields() throws Exception {
        EmployeeCreateRequest request = objectMapper.readValue("""
                {
                  "firstName": "Venkatesh",
                  "email": "dandigam@gmail.com",
                  "employmentType": "Contract",
                  "workMode": "Onsite",
                  "vendorId": 7
                }
                """, EmployeeCreateRequest.class);

        assertEquals("Contract", request.getEmploymentType());
        assertEquals("Onsite", request.getWorkMode());
        assertEquals(7L, request.getVendorId());
    }

    @Test
    void hierarchyResponseUsesOnlyFlatEmployeesList() throws Exception {
        EmployeeHierarchyResponse response = EmployeeHierarchyResponse.builder()
                .viewerEmployeeId(5L)
                .viewerEmployeeName("Srini N")
                .roleType("MANAGER")
                .cycleId(6L)
                .cycleName("RIT 2026 - July | Reviews")
                .employees(List.of(EmployeeHierarchyMemberResponse.builder()
                        .employeeId(4L).employeeName("Dinakar kalaga").managerId(5L).build()))
                .build();

        var json = objectMapper.readTree(objectMapper.writeValueAsString(response));
        assertTrue(json.has("employees"));
        assertEquals(1, json.get("employees").size());
        assertFalse(json.has("currentEmployee"));
        assertFalse(json.has("teamLeads"));
        assertFalse(json.get("employees").get(0).has("employees"));
    }

    @Test
    void csxEmployeeResponseDoesNotExposeEmployeeNumber() throws Exception {
        CsxEmployeeResponse response = CsxEmployeeResponse.builder()
                .id(4L)
                .firstName("John")
                .lastName("Smith")
                .employeeName("John Smith")
                .email("john.smith@csx.com")
                .phoneNumber("9045550104")
                .designationId(25L)
                .designationName("Project Manager")
                .businessUnitId(48L)
                .businessUnitName("CSX")
                .status("ACTIVE")
                .build();

        var json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertFalse(json.has("employeeNumber"));
        assertEquals(25L, json.get("designationId").asLong());
        assertEquals(48L, json.get("businessUnitId").asLong());
        assertEquals("CSX", json.get("businessUnitName").asText());
    }

    @Test
    void readsCsxEmployeeUpdatePayload() throws Exception {
        CsxEmployeeUpdateRequest request = objectMapper.readValue("""
                {
                  "id": 112,
                  "firstName": "Kalyan",
                  "lastName": "Kandlakunta",
                  "email": "kalyan_chakravarthy@csx.com",
                  "phoneNumber": "904-534-3799",
                  "designationId": 22,
                  "businessUnitId": 48,
                  "status": "ACTIVE"
                }
                """, CsxEmployeeUpdateRequest.class);

        assertEquals(112L, request.getId());
        assertEquals(22L, request.getDesignationId());
        assertEquals(48L, request.getBusinessUnitId());
    }

    @Test
    void readsDocumentCreatePayload() throws Exception {
        DocumentCreateRequest request = objectMapper.readValue("""
                {
                  "documentName": "SOW Agreement",
                  "fileType": "application/pdf",
                  "fileUrl": "/documents/sow-agreement.pdf",
                  "module": "SOW"
                }
                """, DocumentCreateRequest.class);

        assertEquals("SOW Agreement", request.getDocumentName());
        assertEquals("application/pdf", request.getFileType());
        assertEquals("SOW", request.getModule());
    }

    @Test
    void readsSowDocumentList() throws Exception {
        com.rit.performance.dto.request.SowRequest request = objectMapper.readValue("""
                {
                  "sowCode": "SOW-008",
                  "sowName": "CSX SOW",
                  "businessUnitId": 48,
                  "documentList": [
                    {
                      "id": 105,
                      "documentName": "agreement.pdf",
                      "fileType": "application/pdf",
                      "fileUrl": "C:\\\\documents\\\\agreement.pdf",
                      "module": "SOW"
                    },
                    {
                      "id": 106,
                      "fileType": "application/pdf"
                    }
                  ]
                }
                """, com.rit.performance.dto.request.SowRequest.class);

        assertEquals(List.of(105L, 106L), request.getDocumentList().stream()
                .map(document -> document.getId())
                .toList());
        assertEquals("application/pdf", request.getDocumentList().get(0).getFileType());
    }
}
