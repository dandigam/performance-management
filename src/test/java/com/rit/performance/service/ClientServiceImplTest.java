package com.rit.performance.service;

import com.rit.performance.dto.ClientDocumentRequest;
import com.rit.performance.dto.ClientRequest;
import com.rit.performance.entity.Client;
import com.rit.performance.entity.Document;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.ClientRepository;
import com.rit.performance.repository.DocumentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientServiceImplTest {
    @Test
    void createsClientWithMsaAndOtherDocuments() {
        ClientRepository repository = mock(ClientRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        ClientServiceImpl service = new ClientServiceImpl(repository, documentRepository);
        ClientRequest request = request("EMAIL", "invoices@csx.com");
        request.setDocumentList(List.of(document(11L), document(12L)));
        when(documentRepository.findAllById(any())).thenReturn(List.of(
                Document.builder().id(11L).module("CLIENT").documentType("MSA").build(),
                Document.builder().id(12L).module("CLIENT").documentType("OTHER_DOCUMENT").build()));
        when(repository.save(any(Client.class))).thenAnswer(invocation -> {
            Client client = invocation.getArgument(0);
            client.setId(7L);
            return client;
        });

        var response = service.create(request);

        assertEquals(7L, response.getId());
        assertEquals("EMAIL", response.getInvoiceSubmissionType());
        assertEquals("invoices@csx.com", response.getInvoiceSubmissionEmail());
        assertEquals(2, response.getDocumentList().size());
    }

    @Test
    void emailSubmissionRequiresInvoiceEmail() {
        ClientServiceImpl service = new ClientServiceImpl(
                mock(ClientRepository.class), mock(DocumentRepository.class));

        assertThrows(InvalidOperationException.class,
                () -> service.create(request("EMAIL", null)));
    }

    @Test
    void rejectsNonClientDocument() {
        ClientRepository repository = mock(ClientRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        ClientServiceImpl service = new ClientServiceImpl(repository, documentRepository);
        ClientRequest request = request("PORTAL", null);
        request.setDocumentList(List.of(document(11L)));
        when(documentRepository.findAllById(any())).thenReturn(List.of(
                Document.builder().id(11L).module("VENDOR").documentType("MSA").build()));

        assertThrows(InvalidOperationException.class, () -> service.create(request));
    }

    private ClientRequest request(String invoiceType, String invoiceEmail) {
        ClientRequest request = new ClientRequest();
        request.setClientName("CSX Transportation");
        request.setClientAddress("500 Water Street, Jacksonville, FL");
        request.setProcurementPersonName("Jane Smith");
        request.setProcurementContactNumber("+1 904 555 0100");
        request.setProcurementEmail("procurement@csx.com");
        request.setInvoiceSubmissionType(invoiceType);
        request.setInvoiceSubmissionEmail(invoiceEmail);
        request.setVmoName("John Manager");
        request.setVmoContactNumber("+1 904 555 0101");
        request.setVmoEmail("vmo@csx.com");
        return request;
    }

    private ClientDocumentRequest document(Long id) {
        ClientDocumentRequest request = new ClientDocumentRequest();
        request.setId(id);
        return request;
    }
}
