package com.rit.performance.service;

import com.rit.performance.dto.request.SowRequest;
import com.rit.performance.dto.response.SowResponse;

import java.util.List;

public interface SowService {
    SowResponse create(SowRequest request);
    List<SowResponse> getAll();
    SowResponse getById(Long id);
    SowResponse update(Long id, SowRequest request);
    void delete(Long id);
}
