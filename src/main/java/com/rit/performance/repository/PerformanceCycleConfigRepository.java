package com.rit.performance.repository;

import com.rit.performance.entity.PerformanceCycles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerformanceCycleConfigRepository extends JpaRepository<PerformanceCycles, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cycle from PerformanceCycles cycle where cycle.id = :id")
    Optional<PerformanceCycles> findForPublication(@Param("id") Long id);

    boolean existsByCycleName(String cycleName);

    Optional<PerformanceCycles> findByCycleName(String cycleName);
}
