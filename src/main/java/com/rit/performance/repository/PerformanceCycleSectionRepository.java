package com.rit.performance.repository;

import com.rit.performance.entity.PerformanceCycleSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerformanceCycleSectionRepository extends JpaRepository<PerformanceCycleSection, Long> {

    List<PerformanceCycleSection> findByPerformanceCycleIdOrderByDisplayOrderAsc(Long performanceCycleId);

    void deleteByPerformanceCycleId(Long performanceCycleId);
}
