package com.rit.performance.service;

import com.rit.performance.dto.VendorRequest;
import com.rit.performance.dto.VendorResponse;

import java.util.List;

public interface VendorService {
    VendorResponse create(VendorRequest request);
    VendorResponse update(Long id, VendorRequest request);
    List<VendorResponse> getAll();
    VendorResponse getById(Long id);
}
