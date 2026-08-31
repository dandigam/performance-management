package com.rit.performance.service;

import com.rit.performance.dto.response.SowResponse;
import com.rit.performance.entity.CsxEmployee;
import com.rit.performance.entity.Document;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.SowMilestone;
import com.rit.performance.mapper.SowMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SowMapperTest {

    @Test
    void includesContactNamesAndEmails() {
        CsxEmployee contact = CsxEmployee.builder()
                .id(112L)
                .firstName("Kalyan")
                .lastName("Kandlakunta")
                .email("kalyan_chakravarthy@csx.com")
                .build();
        CsxEmployee escalation = CsxEmployee.builder()
                .id(113L)
                .firstName("Ava")
                .lastName("Williams")
                .email("ava.williams@csx.com")
                .build();
        CsxEmployee projectOwner = CsxEmployee.builder()
                .id(114L)
                .firstName("Michael")
                .lastName("Johnson")
                .email("michael.johnson@csx.com")
                .build();
        Employee ritContact = new Employee();
        ritContact.setId(5L);
        ritContact.setFirstName("RIT");
        ritContact.setLastName("Manager");
        ritContact.setEmail("manager@rit.com");
        Sow sow = Sow.builder()
                .id(8L)
                .createdBy(10L)
                .createdOn(LocalDateTime.of(2026, 8, 28, 10, 30))
                .updatedBy(11L)
                .updatedOn(LocalDateTime.of(2026, 8, 29, 9, 15))
                .sowCode("SOW-008")
                .sowName("Test SOW")
                .projectOwnerEmployeeId(114L)
                .csxContactEmployeeId(112L)
                .csxEscalationEmployeeId(113L)
                .ritContactEmployee(ritContact)
                .milestones(java.util.Set.of(
                        SowMilestone.builder()
                                .id(201L)
                                .milestoneName("Requirements Completion")
                                .description("Approved requirements and acceptance criteria")
                                .startDate(java.time.LocalDate.of(2026, 8, 1))
                                .endDate(java.time.LocalDate.of(2026, 8, 15))
                                .amount(new java.math.BigDecimal("2500.00"))
                                .status("NOT_STARTED")
                                .build()))
                .documents(java.util.Set.of(
                        Document.builder()
                                .id(105L)
                                .documentName("agreement.pdf")
                                .fileType("application/pdf")
                                .fileUrl("C:\\documents\\agreement.pdf")
                                .module("SOW")
                                .build(),
                        Document.builder().id(106L).documentName("scope.docx").build()))
                .build();

        SowResponse response = SowMapper.toResponse(
                sow,
                Map.of(112L, contact, 113L, escalation, 114L, projectOwner),
                Map.of(10L, "Creator Name", 11L, "Updater Name"));

        assertEquals(114L, response.getProjectOwnerEmployeeId());
        assertEquals("Michael Johnson", response.getProjectOwnerEmployeeName());
        assertEquals("michael.johnson@csx.com", response.getProjectOwnerEmployeeEmail());
        assertEquals("Kalyan Kandlakunta", response.getCsxContactEmployeeName());
        assertEquals("kalyan_chakravarthy@csx.com", response.getCsxContactEmployeeEmail());
        assertEquals("ava.williams@csx.com", response.getCsxEscalationEmployeeEmail());
        assertEquals("manager@rit.com", response.getRitContactEmployeeEmail());
        assertEquals("Approved requirements and acceptance criteria",
                response.getMilestones().get(0).getDescription());
        assertEquals(new java.math.BigDecimal("2500.00"),
                response.getMilestones().get(0).getAmount());
        assertEquals(java.util.List.of(105L, 106L), response.getDocumentList().stream()
                .map(document -> document.getId())
                .toList());
        assertEquals("application/pdf", response.getDocumentList().get(0).getFileType());
        assertEquals("C:\\documents\\agreement.pdf",
                response.getDocumentList().get(0).getFileUrl());
        assertEquals(10L, response.getAudit().createdBy());
        assertEquals("Creator Name", response.getAudit().createdByName());
        assertEquals(LocalDateTime.of(2026, 8, 28, 10, 30),
                response.getAudit().createdOn());
        assertEquals(11L, response.getAudit().updatedBy());
        assertEquals("Updater Name", response.getAudit().updatedByName());
        assertEquals(LocalDateTime.of(2026, 8, 29, 9, 15),
                response.getAudit().updatedOn());
    }
}
