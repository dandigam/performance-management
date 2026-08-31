package com.rit.performance.mapper;

import com.rit.performance.dto.DocumentResponse;
import com.rit.performance.dto.response.SowMilestoneResponse;
import com.rit.performance.dto.response.SowResponse;
import com.rit.performance.dto.response.SowMilestonePositionResponse;
import com.rit.performance.entity.CsxEmployee;
import com.rit.performance.entity.Document;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.SowMilestone;
import com.rit.performance.entity.SowMilestonePosition;

import java.util.Comparator;
import java.util.Map;
import java.util.List;

public final class SowMapper {
    private SowMapper() {
    }

    public static SowResponse toResponse(
            Sow sow,
            Map<Long, CsxEmployee> csxEmployees,
            Map<Long, String> auditorNames) {
        Employee ritContact = sow.getRitContactEmployee();
        Employee ritEscalation = sow.getRitEscalationEmployee();
        CsxEmployee projectOwner = findCsxEmployee(
                csxEmployees, sow.getProjectOwnerEmployeeId());
        CsxEmployee csxContact = findCsxEmployee(csxEmployees, sow.getCsxContactEmployeeId());
        CsxEmployee csxEscalation = findCsxEmployee(
                csxEmployees, sow.getCsxEscalationEmployeeId());
        return SowResponse.builder()
                .id(sow.getId())
                .sowCode(sow.getSowCode())
                .sowName(sow.getSowName())
                .year(sow.getYear())
                .clientId(sow.getClient() == null ? null : sow.getClient().getId())
                .clientName(sow.getClient() == null ? null : sow.getClient().getClientName())
                .sowType(sow.getSowType())
                .engagementType(sow.getEngagementType())
                .businessUnitId(sow.getBusinessUnit() == null ? null : sow.getBusinessUnit().getId())
                .businessUnitName(sow.getBusinessUnit() == null ? null : sow.getBusinessUnit().getName())
                .submittedDate(sow.getSubmittedDate())
                .csxProjectId(sow.getCsxProjectId())
                .projectOwnerEmployeeId(sow.getProjectOwnerEmployeeId())
                .projectOwnerEmployeeName(employeeName(projectOwner))
                .projectOwnerEmployeeEmail(projectOwner == null ? null : projectOwner.getEmail())
                .csxContactEmployeeId(sow.getCsxContactEmployeeId())
                .csxContactEmployeeName(employeeName(csxContact))
                .csxContactEmployeeEmail(csxContact == null ? null : csxContact.getEmail())
                .csxEscalationEmployeeId(sow.getCsxEscalationEmployeeId())
                .csxEscalationEmployeeName(employeeName(csxEscalation))
                .csxEscalationEmployeeEmail(csxEscalation == null ? null : csxEscalation.getEmail())
                .ritContactEmployeeId(ritContact == null ? null : ritContact.getId())
                .ritContactEmployeeName(employeeName(ritContact))
                .ritContactEmployeeEmail(ritContact == null ? null : ritContact.getEmail())
                .ritEscalationEmployeeId(ritEscalation == null ? null : ritEscalation.getId())
                .ritEscalationEmployeeName(employeeName(ritEscalation))
                .ritEscalationEmployeeEmail(ritEscalation == null
                        ? null : ritEscalation.getEmail())
                .startDate(sow.getStartDate())
                .endDate(sow.getEndDate())
                .status(sow.getStatus())
                .remarks(sow.getRemarks())
                .signedStatus(sow.getSignedStatus() == null ? "UNSIGNED" : sow.getSignedStatus())
                .signedDate(sow.getSignedDate())
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
                .audit(AuditMapper.toResponse(sow, auditorNames))
                .build();
    }

    private static DocumentResponse toDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .documentName(document.getDocumentName())
                .fileType(document.getFileType())
                .documentType(document.getDocumentType())
                .fileUrl(document.getFileUrl())
                .module(document.getModule())
                .uploadedAt(document.getUploadedAt())
                .build();
    }

    private static SowMilestoneResponse toMilestoneResponse(SowMilestone milestone) {
        return SowMilestoneResponse.builder()
                .id(milestone.getId())
                .milestoneName(milestone.getMilestoneName())
                .description(milestone.getDescription())
                .deliverables(milestone.getDeliverables())
                .estimatedHours(milestone.getEstimatedHours())
                .startDate(milestone.getStartDate())
                .endDate(milestone.getEndDate())
                .invoiceDate(milestone.getInvoiceDate())
                .amount(milestone.getAmount())
                .status(milestone.getStatus())
                .positions(toPositionResponses(milestone))
                .createdBy(milestone.getCreatedBy())
                .createdDate(milestone.getCreatedOn())
                .updatedBy(milestone.getUpdatedBy())
                .updatedDate(milestone.getUpdatedOn())
                .build();
    }

    private static java.util.List<SowMilestonePositionResponse> toPositionResponses(
            SowMilestone milestone) {
        return milestone.getPositions().stream()
                        .sorted(Comparator.comparing(SowMilestonePosition::getPositionName,
                                String.CASE_INSENSITIVE_ORDER))
                        .map(position -> SowMilestonePositionResponse.builder()
                                .milestonePositionId(position.getId())
                                .positionId(position.getPosition() == null
                                        ? null : position.getPosition().getId())
                                .positionName(position.getPositionName())
                                .seniority(position.getSeniority())
                                .rateCardId(position.getRateCard() == null
                                        ? null : position.getRateCard().getId())
                                .hourlyRate(position.getHourlyRate() != null
                                        ? position.getHourlyRate()
                                        : position.getRateCard() == null ? null
                                        : position.getRateCard().getHourlyRate())
                                .rateOverrideReason(position.getRateOverrideReason())
                                .rateUpdatedBy(position.getRateUpdatedBy())
                                .rateUpdatedDate(position.getRateUpdatedDate())
                                .currency(position.getRateCard() == null
                                        ? null : position.getRateCard().getCurrency())
                                .positionType(position.getPositionType())
                                .locationType(position.getLocationType())
                                .startDate(position.getStartDate())
                                .endDate(position.getEndDate())
                                .hours(position.getHours())
                                .amount(position.getAmount())
                                .assignments(List.of())
                                .build())
                        .toList();
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
