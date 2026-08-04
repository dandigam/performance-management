package com.rit.performance.repository;

import com.rit.performance.entity.PerformanceCycleQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerformanceCycleQuestionRepository extends JpaRepository<PerformanceCycleQuestion, Long> {

    List<PerformanceCycleQuestion> findByPerformanceCycleSectionIdOrderByDisplayOrderAsc(
            Long performanceCycleSectionId);

    void deleteByPerformanceCycleSectionId(Long performanceCycleSectionId);
}
