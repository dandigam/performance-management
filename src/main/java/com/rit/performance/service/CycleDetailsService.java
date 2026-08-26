package com.rit.performance.service;

import com.rit.performance.dto.CycleDetailsRequest;
import com.rit.performance.dto.CycleDetailsResponse;
import com.rit.performance.dto.ReviewCycleRequest;
import com.rit.performance.dto.ReviewCycleResponse;

import java.util.List;

public interface CycleDetailsService {

    CycleDetailsResponse createCycleDetails(ReviewCycleRequest request);

    CycleDetailsResponse updateCycleDetails(Long id, CycleDetailsRequest request);

    List<CycleDetailsResponse> getAllCycleDetails();

    List<ReviewCycleResponse> getAllReviewCycles();

    ReviewCycleResponse getReviewCycleById(Long id);
}
