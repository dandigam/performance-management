package com.rit.performance.service.impl;

import com.rit.performance.controller.SowInvoiceController;
import com.rit.performance.dto.request.SowInvoiceStatusRequest;
import com.rit.performance.entity.*;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SowInvoiceStatusTest {
    @Mock SowInvoiceRepository invoices;
    @Mock SowInvoicePaymentRepository payments;
    @Mock SowInvoiceHistoryRepository history;
    @Mock SowInvoicePaymentHistoryRepository paymentHistory;
    @Mock UserRepository users;
    @Mock SowMilestoneRepository milestones;
    @InjectMocks SowInvoiceServiceImpl service;

    private SowInvoiceStatusRequest request() {
        return SowInvoiceStatusRequest.builder().status("REJECTED")
                .actionDate(LocalDate.of(2026, 9, 11))
                .reason("Client requested a corrected invoice amount.").build();
    }

    @Test
    void endpointPreservesInvoiceAndExposesAuditDetails() throws Exception {
        SowInvoice invoice = SowInvoice.builder().id(123L)
                .sow(Sow.builder().id(1L).build())
                .milestone(SowMilestone.builder().id(2L).build())
                .invoiceStatus("SUBMITTED").invoiceRaisedAmount(new BigDecimal("500.00"))
                .submittedDate(LocalDate.of(2026, 9, 1)).notes("Existing notes").build();
        when(invoices.findByIdWithDetails(123L)).thenReturn(Optional.of(invoice));
        when(invoices.save(any())).thenAnswer(call -> call.getArgument(0));
        MockMvcBuilders.standaloneSetup(new SowInvoiceController(service)).build()
                .perform(post("/api/v1/sow-invoices/123/status")
                        .contentType("application/json")
                        .content("""
                                {"status":"REJECTED","actionDate":"2026-09-11",
                                 "reason":"Client requested a corrected invoice amount."}
                                """))
                .andExpect(status().isOk());
        assertEquals("REJECTED", invoice.getInvoiceStatus());
        assertEquals(new BigDecimal("500.00"), invoice.getInvoiceRaisedAmount());
        assertEquals("Existing notes", invoice.getNotes());
        assertEquals(LocalDate.of(2026, 9, 1), invoice.getSubmittedDate());
        ArgumentCaptor<SowInvoiceHistory> captured = ArgumentCaptor.forClass(SowInvoiceHistory.class);
        verify(history).save(captured.capture());
        SowInvoiceHistory entry = captured.getValue();
        assertEquals("SUBMITTED", entry.getPreviousStatus());
        assertEquals("REJECTED", entry.getAction());
        assertEquals(request().getActionDate(), entry.getActionDate());
        assertEquals(request().getReason(), entry.getReason());
        when(invoices.existsById(123L)).thenReturn(true);
        when(history.findByInvoice_IdOrderByChangedOnDescIdDesc(123L)).thenReturn(List.of(entry));
        var response = service.getHistory(123L).get(0);
        assertEquals(entry.getActionDate(), response.getActionDate());
        assertEquals(entry.getReason(), response.getReason());
        assertEquals(entry.getPreviousStatus(), response.getPreviousStatus());
    }

    @Test
    void invalidStatusDoesNotWrite() {
        var request = request();
        request.setStatus("INVALID");
        assertThrows(InvalidOperationException.class, () -> service.updateStatus(123L, request));
        verifyNoInteractions(invoices, history);
    }

    @Test
    void missingInvoiceDoesNotWriteHistory() {
        when(invoices.findByIdWithDetails(123L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updateStatus(123L, request()));
        verifyNoInteractions(history);
    }

    @Test
    void validatesRequiredFieldsAndReasonLength() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(request()).isEmpty());
            var invalid = request();
            invalid.setStatus(" ");
            invalid.setActionDate(null);
            invalid.setReason("x".repeat(1001));
            assertEquals(3, validator.validate(invalid).size());
        }
    }
}
