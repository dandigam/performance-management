package com.rit.performance.service;

import com.rit.performance.dto.CyclePublishResponse;
import com.rit.performance.dto.CyclePublishRequest;
import com.rit.performance.dto.CyclePublishCandidatesResponse;

public interface ReviewCyclePublishService {
    CyclePublishResponse publish(Long cycleId, Long publishedBy, CyclePublishRequest request);
    CyclePublishCandidatesResponse getPublishCandidates(Long cycleId);
}
