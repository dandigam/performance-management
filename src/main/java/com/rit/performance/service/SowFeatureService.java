package com.rit.performance.service;

import com.rit.performance.dto.request.SowFeatureRequest;
import com.rit.performance.dto.response.SowFeatureResponse;

import java.util.List;

public interface SowFeatureService {
    SowFeatureResponse create(Long sowId, SowFeatureRequest request);
    List<SowFeatureResponse> getAll(Long sowId);
    SowFeatureResponse getById(Long sowId, Long featureId);
    SowFeatureResponse update(Long sowId, Long featureId, SowFeatureRequest request);
    void delete(Long sowId, Long featureId);
}
