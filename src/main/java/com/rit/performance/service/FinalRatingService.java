package com.rit.performance.service;

import com.rit.performance.dto.FinalRatingResponse;

import java.util.List;

public interface FinalRatingService {

    List<FinalRatingResponse> getAllFinalRatings();

    FinalRatingResponse getFinalRatingById(Long id);

    FinalRatingResponse publishRating(Long employeeReviewId, Long publishedById);

    FinalRatingResponse getMyRating(Long employeeId, Long cycleId);
}
