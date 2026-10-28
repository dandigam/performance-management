package com.rit.performance.repository;

import com.rit.performance.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeatureRepository extends JpaRepository<Feature, Long> {
    List<Feature> findAllByOrderByCodeAsc();
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
}
