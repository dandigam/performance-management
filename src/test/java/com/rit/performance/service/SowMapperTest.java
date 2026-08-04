package com.rit.performance.service;

import com.rit.performance.dto.response.SowResponse;
import com.rit.performance.entity.CsxEmployee;
import com.rit.performance.entity.Document;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.Sow;
import com.rit.performance.mapper.SowMapper;
import org.junit.jupiter.api.Test;

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
        Employee ritContact = new Employee();
        ritContact.setId(5L);
        ritContact.setFirstName("RIT");
        ritContact.setLastName("Manager");
        ritContact.setEmail("manager@rit.com");
        Sow sow = Sow.builder()
                .id(8L)
                .sowCode("SOW-008")
                .sowName("Test SOW")
                .csxContactEmployeeId(112L)
                .csxEscalationEmployeeId(113L)
                .ritContactEmployee(ritContact)
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
                sow, Map.of(112L, contact, 113L, escalation));

        assertEquals("Kalyan Kandlakunta", response.getCsxContactEmployeeName());
        assertEquals("kalyan_chakravarthy@csx.com", response.getCsxContactEmployeeEmail());
        assertEquals("ava.williams@csx.com", response.getCsxEscalationEmployeeEmail());
        assertEquals("manager@rit.com", response.getRitContactEmployeeEmail());
        assertEquals(java.util.List.of(105L, 106L), response.getDocumentList().stream()
                .map(document -> document.getId())
                .toList());
        assertEquals("application/pdf", response.getDocumentList().get(0).getFileType());
        assertEquals("C:\\documents\\agreement.pdf",
                response.getDocumentList().get(0).getFileUrl());
    }
}
