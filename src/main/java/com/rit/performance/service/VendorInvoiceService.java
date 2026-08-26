package com.rit.performance.service;

import com.rit.performance.dto.request.VendorInvoiceRequest;
import com.rit.performance.dto.response.VendorInvoiceResponse;

import java.util.List;

public interface VendorInvoiceService {
    VendorInvoiceResponse create(VendorInvoiceRequest request);
    List<VendorInvoiceResponse> getAll();
    VendorInvoiceResponse getById(Long id);
    VendorInvoiceResponse update(Long id, VendorInvoiceRequest request);
    void delete(Long id);
}
