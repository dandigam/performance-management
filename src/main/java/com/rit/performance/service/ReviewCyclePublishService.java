package com.rit.performance.service;

import com.rit.performance.dto.CyclePublishResponse;

public interface ReviewCyclePublishService {
    CyclePublishResponse publish(Long cycleId, Long publishedBy);
}
