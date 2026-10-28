package com.rit.performance.repository;

import com.rit.performance.entity.RoleFeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

public interface RoleFeatureFlagRepository extends JpaRepository<RoleFeatureFlag, Long> {
    @EntityGraph(attributePaths = {"feature"})
    List<RoleFeatureFlag> findByRoleId(Long roleId);
    Optional<RoleFeatureFlag> findByRoleIdAndFeatureId(Long roleId, Long featureId);
}
