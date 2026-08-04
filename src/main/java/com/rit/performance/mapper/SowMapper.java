package com.rit.performance.mapper;

import com.rit.performance.dto.DocumentResponse;
import com.rit.performance.dto.response.SowMilestoneResponse;
import com.rit.performance.dto.response.SowResponse;
import com.rit.performance.entity.CsxEmployee;
import com.rit.performance.entity.Document;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.SowMilestone;

import java.util.Comparator;
import java.util.Map;

public final class SowMapper {
    private SowMapper() {
    }

    public static SowResponse toResponse(Sow sow, Map<Long, CsxEmployee> csxEmployees) {
        Employee ritContact = sow.getRitContactEmployee();
        CsxEmployee csxContact = findCsxEmployee(csxEmployees, sow.getCsxContactEmployeeId());
        CsxEmployee csxEscalation = findCsxEmployee(
                csxEmployees, sow.getCsxEscalationEmployeeId());
        return SowResponse.builder()
                .id(sow.getId())
                .sowCode(sow.getSowCode())
                .sowName(sow.getSowName())
                .businessUnitId(sow.getBusinessUnit() == null ? null : sow.getBusinessUnit().getId())
                .businessUnitName(sow.getBusinessUnit() == null ? null : sow.getBusinessUnit().getName())
                .submittedDate(sow.getSubmittedDate())
                .csxProjectId(sow.getCsxProjectId())
                .csxContactEmployeeId(sow.getCsxContactEmployeeId())
                .csxContactEmployeeName(employeeName(csxContact))
                .csxContactEmployeeEmail(csxContact == null ? null : csxContact.getEmail())
                .csxEscalationEmployeeId(sow.getCsxEscalationEmployeeId())
                .csxEscalationEmployeeName(employeeName(csxEscalation))
                .csxEscalationEmployeeEmail(csxEscalation == null ? null : csxEscalation.getEmail())
                .ritContactEmployeeId(ritContact == null ? null : ritContact.getId())
                .ritContactEmployeeName(employeeName(ritContact))
                .ritContactEmployeeEmail(ritContact == null ? null : ritContact.getEmail())
                .startDate(sow.getStartDate())
                .endDate(sow.getEndDate())
                .status(sow.getStatus())
                .milestones(sow.getMilestones().stream()
                        .sorted(Comparator.comparing(
                                SowMilestone::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(SowMapper::toMilestoneResponse)
                        .toList())
                .documentList(sow.getDocuments().stream()
                        .sorted(Comparator.comparing(
                                Document::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(SowMapper::toDocumentResponse)
                        .toList())
                .createdBy(sow.getCreatedBy())
                .createdDate(sow.getCreatedDate())
                .updatedBy(sow.getUpdatedBy())
                .updatedDate(sow.getUpdatedDate())
                .build();
    }

    private static DocumentResponse toDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .documentName(document.getDocumentName())
                .fileType(document.getFileType())
                .fileUrl(document.getFileUrl())
                .module(document.getModule())
                .uploadedAt(document.getUploadedAt())
                .build();
    }

    private static SowMilestoneResponse toMilestoneResponse(SowMilestone milestone) {
        return SowMilestoneResponse.builder()
                .id(milestone.getId())
                .milestoneName(milestone.getMilestoneName())
                .startDate(milestone.getStartDate())
                .endDate(milestone.getEndDate())
                .invoiceDate(milestone.getInvoiceDate())
                .amount(milestone.getAmount())
                .status(milestone.getStatus())
                .createdBy(milestone.getCreatedBy())
                .createdDate(milestone.getCreatedDate())
                .updatedBy(milestone.getUpdatedBy())
                .updatedDate(milestone.getUpdatedDate())
                .build();
    }

    private static String employeeName(Employee employee) {
        if (employee == null) return null;
        String firstName = employee.getFirstName() == null ? "" : employee.getFirstName().trim();
        String lastName = employee.getLastName() == null ? "" : employee.getLastName().trim();
        return (firstName + " " + lastName).trim();
    }

    private static String employeeName(CsxEmployee employee) {
        if (employee == null) return null;
        String firstName = employee.getFirstName() == null ? "" : employee.getFirstName().trim();
        String lastName = employee.getLastName() == null ? "" : employee.getLastName().trim();
        return (firstName + " " + lastName).trim();
    }

    private static CsxEmployee findCsxEmployee(
            Map<Long, CsxEmployee> csxEmployees, Long employeeId
    ) {
        return employeeId == null ? null : csxEmployees.get(employeeId);
    }
}
