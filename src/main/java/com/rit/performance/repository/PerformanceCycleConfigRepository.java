package com.rit.performance.repository;

import com.rit.performance.entity.PerformanceCycles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerformanceCycleConfigRepository extends JpaRepository<PerformanceCycles, Long> {

    boolean existsByCycleName(String cycleName);

    Optional<PerformanceCycles> findByCycleName(String cycleName);
}
