package com.rit.performance.service;

import com.rit.performance.dto.request.SowInvoiceRequest;
import com.rit.performance.dto.response.SowInvoiceResponse;
import com.rit.performance.dto.request.SowInvoicePaymentRequest;
import com.rit.performance.dto.response.SowInvoicePaymentResponse;
import com.rit.performance.dto.response.SowInvoiceHistoryResponse;
import com.rit.performance.dto.response.SowInvoicePaymentHistoryResponse;
import com.rit.performance.dto.response.SowInvoiceAuditHistoryResponse;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.SowMilestone;

import java.util.Collection;
import java.util.List;

public interface SowInvoiceService {
    List<SowInvoiceResponse> getAll(Long sowId, String invoiceStatus, String paymentStatus);
    SowInvoiceResponse getById(Long id);
    SowInvoiceResponse create(SowInvoiceRequest request);
    SowInvoiceResponse update(Long id, SowInvoiceRequest request);
    List<SowInvoicePaymentResponse> getPayments(Long invoiceId);
    SowInvoicePaymentResponse createPayment(Long invoiceId, SowInvoicePaymentRequest request);
    SowInvoicePaymentResponse updatePayment(
            Long invoiceId, Long paymentId, SowInvoicePaymentRequest request);
    void deletePayment(Long invoiceId, Long paymentId);
    List<SowInvoiceHistoryResponse> getHistory(Long invoiceId);
    List<SowInvoicePaymentHistoryResponse> getPaymentHistory(Long invoiceId);
    SowInvoiceAuditHistoryResponse getAuditHistory(Long invoiceId);
    void createDraftInvoices(Sow sow, Collection<SowMilestone> milestones);
}
