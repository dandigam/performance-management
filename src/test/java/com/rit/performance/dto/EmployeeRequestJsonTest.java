package com.rit.performance.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

class EmployeeRequestJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void vendorContractUsesBankDetailsAndAcceptsLegacyPaymentDetailsAlias() throws Exception {
        VendorRequest request = objectMapper.readValue("""
                {
                  "companyName": "Example LLC",
                  "vendorLocation": "ONSITE",
                  "vendorType": "CONSULTING",
                  "taxIdentifier": "12-3456789",
                  "paymentDetails": {
                    "bankCountry": "US",
                    "currency": "USD",
                    "accountHolderName": "Example LLC",
                    "bankName": "Example Bank",
                    "accountNumber": "1234567890",
                    "routingNumber": "021000021"
                  }
                }
                """, VendorRequest.class);
        assertEquals("Example Bank", request.getBankDetails().getBankName());

        VendorResponse response = VendorResponse.builder()
                .id(7L)
                .bankDetails(VendorBankDetailsResponse.builder()
                        .id(21L)
                        .accountNumberLast4("7890")
                        .build())
                .build();
        var json = objectMapper.readTree(objectMapper.writeValueAsString(response));
        assertTrue(json.has("bankDetails"));
        assertFalse(json.has("paymentDetails"));
        assertFalse(json.has("vendorCode"));
    }

    @Test
    void readsNestedProjectAssignment() throws Exception {
        EmployeeCreateRequest request = objectMapper.readValue("""
                {
                  "firstName": "John",
                  "lastName": "Doe",
                  "email": "john.doe@rit.com",
                  "roleId": 29,
                  "designationId": 42,
                  "projectAssignment": {
                    "departmentId": 48,
                    "designationId": 19,
                    "sowId": 3,
                    "milestoneId": 11,
                    "leadId": 1,
                    "managerId": 5,
                    "positionType": "BILLABLE",
                    "effectiveFrom": "2026-07-15"
                  }
                }
                """, EmployeeCreateRequest.class);

        assertNotNull(request.getProjectAssignment());
        assertEquals(42L, request.getDesignationId());
        assertEquals(19L, request.getProjectAssignment().getDesignationId());
        assertEquals(3L, request.getProjectAssignment().getSowId());
        assertEquals(11L, request.getProjectAssignment().getMilestoneId());
        assertEquals("BILLABLE", request.getProjectAssignment().getPositionType());
        assertEquals(1L, request.getProjectAssignment().getLeadId());
        assertEquals(5L, request.getProjectAssignment().getManagerId());
        assertTrue(request.getProjectAssignment().isLeadIdPresent());
        assertTrue(request.getProjectAssignment().isManagerIdPresent());
    }

    @Test
    void readsProfileDesignationWithoutProjectAssignment() throws Exception {
        EmployeeCreateRequest createRequest = objectMapper.readValue("""
                {
                  "firstName": "Jane",
                  "email": "jane.cto@rit.com",
                  "employmentType": "FULL_TIME",
                  "workMode": "ONSITE",
                  "roleId": 35,
                  "designationId": 42
                }
                """, EmployeeCreateRequest.class);
        EmployeeUpdateRequest updateRequest = objectMapper.readValue("""
                { "designationId": 43, "updatedBy": 1 }
                """, EmployeeUpdateRequest.class);

        assertEquals(42L, createRequest.getDesignationId());
        assertNull(createRequest.getProjectAssignment());
        assertEquals(43L, updateRequest.getDesignationId());
        assertTrue(updateRequest.isDesignationIdPresent());
    }

    @Test
    void readsNestedEmployeeUpdateAssignment() throws Exception {
        EmployeeUpdateRequest request = objectMapper.readValue("""
                {
                  "firstName": "Venkatesh",
                  "ritEmployeeId": "RIT100",
                  "projectAssignment": {
                    "departmentId": 48,
                    "designationId": 19,
                    "sowId": 1,
                    "milestoneId": null,
                    "leadId": 2,
                    "managerId": 5,
                    "positionType": "NON_BILLABLE"
                  }
                }
                """, EmployeeUpdateRequest.class);

        assertEquals("RIT100", request.getRitId());
        assertEquals(19L, request.getProjectAssignment().getDesignationId());
        assertEquals(1L, request.getProjectAssignment().getSowId());
        assertNull(request.getProjectAssignment().getMilestoneId());
        assertTrue(request.getProjectAssignment().isMilestoneIdPresent());
        assertEquals("NON_BILLABLE", request.getProjectAssignment().getPositionType());
        assertEquals(2L, request.getProjectAssignment().getLeadId());
        assertEquals(5L, request.getProjectAssignment().getManagerId());
        assertTrue(request.getProjectAssignment().isDesignationIdPresent());
        assertTrue(request.getProjectAssignment().isSowIdPresent());
    }

    @Test
    void readsEmployeeEmploymentAndVendorFields() throws Exception {
        EmployeeCreateRequest request = objectMapper.readValue("""
                {
                  "firstName": "Venkatesh",
                  "email": "dandigam@gmail.com",
                  "phoneNumber": "+1 404 555 0123",
                  "gender": "MALE",
                  "dateOfBirth": "1995-04-12",
                  "addressDetails": {
                    "addressLine1": "123 Main Street",
                    "addressLine2": "Apt 4B",
                    "city": "Jacksonville",
                    "state": "Florida",
                    "postalCode": "32202",
                    "country": "USA"
                  },
                  "compensationDetails": {
                    "payType": "HOURLY",
                    "hourlyRate": 78.00,
                    "currency": "USD",
                    "effectiveDate": "2026-07-01"
                  },
                  "professionalDetails": {
                    "itSkills": "Angular, Java, SQL, AWS",
                    "latestExperience": "Senior Developer at Example Corp"
                  },
                  "bankDetails": {
                    "bankCountry": "USA",
                    "currency": "USD",
                    "accountHolderName": "Venkatesh Rao",
                    "bankName": "Example Bank",
                    "accountNumber": "1234567890",
                    "ifscCode": "EXAMP000123"
                  },
                  "documentList": [
                    { "id": 41 },
                    { "id": 42 }
                  ],
                  "employmentType": "Contract",
                  "workMode": "Onsite",
                  "vendorId": 7
                }
                """, EmployeeCreateRequest.class);

        assertEquals("Contract", request.getEmploymentType());
        assertEquals("Onsite", request.getWorkMode());
        assertEquals(7L, request.getVendorId());
        assertEquals("+1 404 555 0123", request.getPhoneNumber());
        assertEquals("MALE", request.getGender());
        assertEquals(java.time.LocalDate.of(1995, 4, 12), request.getDateOfBirth());
        assertEquals("123 Main Street", request.getAddressDetails().getAddressLine1());
        assertEquals("Jacksonville", request.getAddressDetails().getCity());
        assertEquals("32202", request.getAddressDetails().getPostalCode());
        assertEquals("HOURLY", request.getCompensationDetails().getPayType());
        assertEquals(new java.math.BigDecimal("78.00"), request.getCompensationDetails().getHourlyRate());
        assertEquals("USD", request.getCompensationDetails().getCurrency());
        assertEquals("Angular, Java, SQL, AWS", request.getProfessionalDetails().getItSkills());
        assertEquals("Senior Developer at Example Corp",
                request.getProfessionalDetails().getLatestExperience());
        assertEquals("USA", request.getBankDetails().getBankCountry());
        assertEquals("USD", request.getBankDetails().getCurrency());
        assertEquals("Venkatesh Rao", request.getBankDetails().getAccountHolderName());
        assertEquals("1234567890", request.getBankDetails().getAccountNumber());
        assertEquals("EXAMP000123", request.getBankDetails().getIfscCode());
        assertEquals(List.of(41L, 42L), request.getDocumentList().stream()
                .map(EmployeeDocumentRequest::getId)
                .toList());
    }

    @Test
    void readsEmployeeExperienceDetailsAsArray() throws Exception {
        EmployeeCreateRequest request = objectMapper.readValue("""
                {
                  "firstName": "Venkatesh",
                  "email": "dandigam@gmail.com",
                  "employmentType": "CONTRACT",
                  "workMode": "ONSITE",
                  "experienceDetails": [
                    {
                      "companyName": "RIT",
                      "position": "Technical Lead",
                      "location": "Jacksonville",
                      "fromDate": "2026-07-01",
                      "endDate": null
                    }
                  ]
                }
                """, EmployeeCreateRequest.class);

        assertEquals(1, request.getExperienceDetails().size());
        assertEquals("RIT", request.getExperienceDetails().get(0).getCompanyName());
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
