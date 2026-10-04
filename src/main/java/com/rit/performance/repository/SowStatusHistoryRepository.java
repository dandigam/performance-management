package com.rit.performance.repository;

import com.rit.performance.entity.SowStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SowStatusHistoryRepository extends JpaRepository<SowStatusHistory, Long> {
    java.util.List<SowStatusHistory> findBySowIdOrderByChangedAtAscIdAsc(Long sowId);
}
