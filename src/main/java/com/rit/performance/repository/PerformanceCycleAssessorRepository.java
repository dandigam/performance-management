package com.rit.performance.repository;

import com.rit.performance.entity.PerformanceCycleAssessor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerformanceCycleAssessorRepository extends JpaRepository<PerformanceCycleAssessor, Long> {

    List<PerformanceCycleAssessor> findByPerformanceCycleIdOrderByDisplayOrderAsc(Long performanceCycleId);

    void deleteByPerformanceCycleId(Long performanceCycleId);
}
