package com.rit.performance.repository;

import com.rit.performance.entity.PerformanceCycleTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerformanceCycleTimelineRepository extends JpaRepository<PerformanceCycleTimeline, Long> {

    List<PerformanceCycleTimeline> findByPerformanceCycleIdOrderByDisplayOrderAsc(Long performanceCycleId);

    List<PerformanceCycleTimeline> findByPerformanceCycleIdInOrderByPerformanceCycleIdAscDisplayOrderAsc(
            List<Long> performanceCycleIds);

    void deleteByPerformanceCycleId(Long performanceCycleId);
}
