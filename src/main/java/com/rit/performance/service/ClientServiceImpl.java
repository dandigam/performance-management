package com.rit.performance.service;

import com.rit.performance.dto.ClientDocumentRequest;
import com.rit.performance.dto.ClientRequest;
import com.rit.performance.dto.ClientResponse;
import com.rit.performance.dto.DocumentResponse;
import com.rit.performance.entity.Client;
import com.rit.performance.entity.Document;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.ClientRepository;
import com.rit.performance.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientServiceImpl implements ClientService {
    private static final Set<String> INVOICE_TYPES = Set.of("PORTAL", "EMAIL");
    private static final Set<String> DOCUMENT_TYPES = Set.of("MSA", "OTHER_DOCUMENT");

    private final ClientRepository clientRepository;
    private final DocumentRepository documentRepository;

    @Override
    @Transactional
    public ClientResponse create(ClientRequest request) {
        String clientName = request.getClientName().trim();
        if (clientRepository.existsByClientNameIgnoreCase(clientName)) {
            throw new DuplicateResourceException("Client name already exists: " + clientName);
        }
        Client client = new Client();
        apply(client, request);
        synchronizeDocuments(client, request.getDocumentList());
        return toResponse(clientRepository.save(client));
    }

    @Override
    @Transactional
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
        String clientName = request.getClientName().trim();
        if (clientRepository.existsByClientNameIgnoreCaseAndIdNot(clientName, id)) {
            throw new DuplicateResourceException("Client name already exists: " + clientName);
        }
        apply(client, request);
        if (request.getDocumentList() != null) {
            synchronizeDocuments(client, request.getDocumentList());
        }
        return toResponse(clientRepository.save(client));
    }

    @Override
    public ClientResponse getById(Long id) {
        return clientRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
    }

    @Override
    public List<ClientResponse> getAll() {
        return clientRepository.findAll(Sort.by(Sort.Direction.ASC, "clientName")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
        client.getDocuments().clear();
        clientRepository.delete(client);
    }

    private void apply(Client client, ClientRequest request) {
        String invoiceType = normalizeUpper(request.getInvoiceSubmissionType());
        if (!INVOICE_TYPES.contains(invoiceType)) {
            throw new InvalidOperationException("invoiceSubmissionType must be PORTAL or EMAIL");
        }
        String invoiceEmail = normalizeEmail(request.getInvoiceSubmissionEmail());
        if ("EMAIL".equals(invoiceType) && invoiceEmail == null) {
            throw new InvalidOperationException(
                    "invoiceSubmissionEmail is required when invoiceSubmissionType is EMAIL");
        }

        client.setClientName(request.getClientName().trim());
        client.setClientAddress(request.getClientAddress().trim());
        client.setProcurementPersonName(request.getProcurementPersonName().trim());
        client.setProcurementContactNumber(request.getProcurementContactNumber().trim());
        client.setProcurementEmail(normalizeEmail(request.getProcurementEmail()));
        client.setInvoiceSubmissionType(invoiceType);
        client.setInvoiceSubmissionEmail("EMAIL".equals(invoiceType) ? invoiceEmail : null);
        client.setVmoName(request.getVmoName().trim());
        client.setVmoContactNumber(request.getVmoContactNumber().trim());
        client.setVmoEmail(normalizeEmail(request.getVmoEmail()));
        client.setStatus(normalizeStatus(request.getStatus()));
    }

    private void synchronizeDocuments(Client client, List<ClientDocumentRequest> documentList) {
        if (documentList == null) return;
        List<Long> requestedIds = documentList.stream().map(ClientDocumentRequest::getId).toList();
        Set<Long> uniqueIds = new LinkedHashSet<>(requestedIds);
        if (uniqueIds.size() != requestedIds.size()) {
            throw new InvalidOperationException("documentList cannot contain duplicate document ids");
        }
        List<Document> documents = uniqueIds.isEmpty() ? List.of() : documentRepository.findAllById(uniqueIds);
        Set<Long> foundIds = documents.stream().map(Document::getId).collect(Collectors.toSet());
        Set<Long> missingIds = new LinkedHashSet<>(uniqueIds);
        missingIds.removeAll(foundIds);
        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException("Documents not found: " + missingIds);
        }
        for (Document document : documents) {
            if (!"CLIENT".equalsIgnoreCase(document.getModule())) {
                throw new InvalidOperationException("Document " + document.getId() + " is not a client document");
            }
            String type = normalizeUpper(document.getDocumentType());
            if (!DOCUMENT_TYPES.contains(type)) {
                throw new InvalidOperationException(
                        "Client document type must be MSA or OTHER_DOCUMENT");
            }
        }
        client.getDocuments().clear();
        client.getDocuments().addAll(documents);
    }

    private ClientResponse toResponse(Client client) {
        return ClientResponse.builder()
                .id(client.getId())
                .clientName(client.getClientName())
                .clientAddress(client.getClientAddress())
                .procurementPersonName(client.getProcurementPersonName())
                .procurementContactNumber(client.getProcurementContactNumber())
                .procurementEmail(client.getProcurementEmail())
                .invoiceSubmissionType(client.getInvoiceSubmissionType())
                .invoiceSubmissionEmail(client.getInvoiceSubmissionEmail())
                .vmoName(client.getVmoName())
                .vmoContactNumber(client.getVmoContactNumber())
                .vmoEmail(client.getVmoEmail())
                .status(client.getStatus())
                .documentList(client.getDocuments().stream()
                        .sorted(Comparator.comparing(Document::getId))
                        .map(this::toDocumentResponse)
                        .toList())
                .createdDate(client.getCreatedDate())
                .updatedDate(client.getUpdatedDate())
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

    private String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String normalizeEmail(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeStatus(String value) {
        String status = value == null || value.isBlank() ? "ACTIVE" : normalizeUpper(value);
        if (!Set.of("ACTIVE", "INACTIVE").contains(status)) {
            throw new InvalidOperationException("status must be ACTIVE or INACTIVE");
        }
        return status;
    }
}
