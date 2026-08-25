package com.rit.performance.repository;

import com.rit.performance.entity.WorkOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    @EntityGraph(attributePaths = {"sow", "employee", "documents"})
    List<WorkOrder> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = {"sow", "employee", "documents"})
    Optional<WorkOrder> findOneById(Long id);
}
