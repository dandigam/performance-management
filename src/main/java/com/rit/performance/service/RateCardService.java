package com.rit.performance.service;

import com.rit.performance.dto.RateCardRequest;
import com.rit.performance.dto.RateCardResponse;
import java.util.List;

public interface RateCardService {
    RateCardResponse create(RateCardRequest request);
    RateCardResponse update(Long id, RateCardRequest request);
    RateCardResponse getById(Long id);
    List<RateCardResponse> getAll();
    void delete(Long id);
}
