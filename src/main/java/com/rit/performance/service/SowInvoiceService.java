package com.rit.performance.service;

import com.rit.performance.dto.request.SowInvoiceRequest;
import com.rit.performance.dto.response.SowInvoiceResponse;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.SowMilestone;

import java.util.Collection;
import java.util.List;

public interface SowInvoiceService {
    List<SowInvoiceResponse> getAll(Long sowId, String invoiceStatus, String paymentStatus);
    SowInvoiceResponse getById(Long id);
    SowInvoiceResponse create(SowInvoiceRequest request);
    SowInvoiceResponse update(Long id, SowInvoiceRequest request);
    void createDraftInvoices(Sow sow, Collection<SowMilestone> milestones);
}
