package com.rit.performance.service.impl;

import com.rit.performance.dto.request.WorkOrderRequest;
import com.rit.performance.dto.response.WorkOrderResponse;
import com.rit.performance.dto.DocumentResponse;
import com.rit.performance.dto.request.WorkOrderDocumentRequest;
import com.rit.performance.entity.Document;
import com.rit.performance.entity.Employee;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.WorkOrder;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.repository.SowRepository;
import com.rit.performance.repository.WorkOrderRepository;
import com.rit.performance.repository.DocumentRepository;
import com.rit.performance.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {
    private final WorkOrderRepository workOrderRepository;
    private final SowRepository sowRepository;
    private final EmployeeRepository employeeRepository;
    private final DocumentRepository documentRepository;

    @Override
    public WorkOrderResponse create(WorkOrderRequest request) {
        WorkOrder workOrder = new WorkOrder();
        apply(workOrder, request);
        return toResponse(workOrderRepository.saveAndFlush(workOrder));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkOrderResponse> getAll() {
        return workOrderRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkOrderResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Override
    public WorkOrderResponse update(Long id, WorkOrderRequest request) {
        WorkOrder workOrder = find(id);
        apply(workOrder, request);
        return toResponse(workOrderRepository.saveAndFlush(workOrder));
    }

    @Override
    public void delete(Long id) {
        workOrderRepository.delete(find(id));
    }

    private void apply(WorkOrder workOrder, WorkOrderRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidOperationException("endDate cannot be before startDate");
        }
        String location = request.getLocation().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ONSITE", "OFFSHORE").contains(location)) {
            throw new InvalidOperationException("location must be ONSITE or OFFSHORE");
        }
        Sow sow = sowRepository.findById(request.getSowId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SOW not found: " + request.getSowId()));
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + request.getEmployeeId()));

        workOrder.setDescription(request.getDescription().trim());
        workOrder.setStartDate(request.getStartDate());
        workOrder.setEndDate(request.getEndDate());
        workOrder.setLocation(location);
        workOrder.setSow(sow);
        workOrder.setAmount(request.getAmount());
        workOrder.setHourlyRate(request.getHourlyRate());
        workOrder.setSalary(request.getSalary());
        workOrder.setCommission(request.getCommission());
        workOrder.setEmployee(employee);
        workOrder.setComments(trimToNull(request.getComments()));
        if (request.getDocumentList() != null) {
            synchronizeDocuments(workOrder, request.getDocumentList());
        }
    }

    private void synchronizeDocuments(
            WorkOrder workOrder, List<WorkOrderDocumentRequest> documentList) {
        List<Long> requestedIds = documentList.stream()
                .map(WorkOrderDocumentRequest::getId)
                .toList();
        Set<Long> uniqueIds = new LinkedHashSet<>(requestedIds);
        if (uniqueIds.size() != requestedIds.size()) {
            throw new InvalidOperationException(
                    "documentList cannot contain duplicate document ids");
        }
        List<Document> documents = uniqueIds.isEmpty()
                ? List.of() : documentRepository.findAllById(uniqueIds);
        Set<Long> foundIds = documents.stream().map(Document::getId)
                .collect(Collectors.toSet());
        Set<Long> missingIds = new LinkedHashSet<>(uniqueIds);
        missingIds.removeAll(foundIds);
        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException("Documents not found: " + missingIds);
        }
        workOrder.getDocuments().clear();
        workOrder.getDocuments().addAll(documents);
    }

    private WorkOrder find(Long id) {
        return workOrderRepository.findOneById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found: " + id));
    }

    private WorkOrderResponse toResponse(WorkOrder workOrder) {
        Sow sow = workOrder.getSow();
        Employee employee = workOrder.getEmployee();
        String employeeName = ((employee.getFirstName() == null ? "" : employee.getFirstName())
                + " " + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
        return WorkOrderResponse.builder()
                .id(workOrder.getId())
                .description(workOrder.getDescription())
                .startDate(workOrder.getStartDate())
                .endDate(workOrder.getEndDate())
                .location(workOrder.getLocation())
                .sowId(sow.getId()).sowCode(sow.getSowCode()).sowName(sow.getSowName())
                .amount(workOrder.getAmount()).hourlyRate(workOrder.getHourlyRate())
                .salary(workOrder.getSalary()).commission(workOrder.getCommission())
                .employeeId(employee.getId()).employeeName(employeeName)
                .comments(workOrder.getComments())
                .documentList(workOrder.getDocuments().stream()
                        .sorted(Comparator.comparing(Document::getId))
                        .map(this::toDocumentResponse)
                        .toList())
                .createdAt(workOrder.getCreatedAt()).updatedAt(workOrder.getUpdatedAt())
                .build();
    }

    private DocumentResponse toDocumentResponse(Document document) {
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

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
