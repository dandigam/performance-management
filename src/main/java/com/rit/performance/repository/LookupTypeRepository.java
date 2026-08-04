package com.rit.performance.repository;

import com.rit.performance.entity.LookupType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface LookupTypeRepository extends JpaRepository<LookupType, Long> {
    Optional<LookupType> findByCodeIgnoreCase(String code);

    List<LookupType> findAllByOrderByIdAsc();

    List<LookupType> findByActiveTrueOrderByIdAsc();
}
