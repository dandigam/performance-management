package com.rit.performance.service.impl;

import com.rit.performance.dto.request.SowInvoiceRequest;
import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SowInvoiceCsxNumberTest {
    @Mock SowInvoiceRepository invoices;
    @Mock SowInvoicePaymentRepository payments;
    @Mock SowInvoiceHistoryRepository history;
    @Mock SowInvoicePaymentHistoryRepository paymentHistory;
    @Mock UserRepository users;
    @Mock SowMilestoneRepository milestones;
    @InjectMocks SowInvoiceServiceImpl service;

    @Test
    void savesNumberAndPreservesHistoricalValueAfterUpdate() {
        Sow sow = Sow.builder().id(1L).build();
        SowMilestone milestone = SowMilestone.builder().id(2L).sow(sow).build();
        when(milestones.findById(2L)).thenReturn(Optional.of(milestone));
        when(invoices.saveAndFlush(any())).thenAnswer(call -> {
            SowInvoice invoice = call.getArgument(0);
            invoice.setId(3L);
            return invoice;
        });
        var request = SowInvoiceRequest.builder().milestoneId(2L)
                .csxInvoiceNumber("  CSX-001  ").build();
        assertEquals("CSX-001", service.create(request).getCsxInvoiceNumber());
        var captured = ArgumentCaptor.forClass(SowInvoiceHistory.class);
        verify(history).save(captured.capture());
        SowInvoiceHistory snapshot = captured.getValue();
        SowInvoice invoice = snapshot.getInvoice();
        when(invoices.findByIdWithDetails(3L)).thenReturn(Optional.of(invoice));
        when(invoices.save(any())).thenAnswer(call -> call.getArgument(0));
        request.setCsxInvoiceNumber("CSX-002");
        assertEquals("CSX-002", service.update(3L, request).getCsxInvoiceNumber());
        assertEquals("CSX-002", service.getById(3L).getCsxInvoiceNumber());
        when(invoices.findAllWithDetails()).thenReturn(List.of(invoice));
        assertEquals("CSX-002", service.getAll(null, null, null).get(0).getCsxInvoiceNumber());
        when(invoices.existsById(3L)).thenReturn(true);
        when(history.findByInvoice_IdOrderByChangedOnDescIdDesc(3L)).thenReturn(List.of(snapshot));
        assertEquals("CSX-001", service.getHistory(3L).get(0).getCsxInvoiceNumber());
        request.setCsxInvoiceNumber("  ");
        assertNull(service.update(3L, request).getCsxInvoiceNumber());
    }

    @Test
    void numberIsOptionalAndLimitedTo100Characters() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var request = SowInvoiceRequest.builder().milestoneId(2L).build();
            var validator = factory.getValidator();
            assertTrue(validator.validate(request).isEmpty());
            request.setCsxInvoiceNumber("x".repeat(100));
            assertTrue(validator.validate(request).isEmpty());
            request.setCsxInvoiceNumber("x".repeat(101));
            assertEquals("csxInvoiceNumber", validator.validate(request).iterator().next().getPropertyPath().toString());
        }
    }
}
