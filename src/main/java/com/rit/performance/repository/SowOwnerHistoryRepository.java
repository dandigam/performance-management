package com.rit.performance.repository;

import com.rit.performance.entity.SowOwnerHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SowOwnerHistoryRepository extends JpaRepository<SowOwnerHistory, Long> {
    List<SowOwnerHistory> findBySowIdOrderByChangedAtAscIdAsc(Long sowId);
    boolean existsBySowIdAndRole(Long sowId, String role);
}
