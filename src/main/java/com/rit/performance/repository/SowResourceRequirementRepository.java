package com.rit.performance.repository;

import com.rit.performance.entity.SowResourceRequirement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SowResourceRequirementRepository
        extends JpaRepository<SowResourceRequirement, Long> {
    @EntityGraph(attributePaths = {"sow", "sow.businessUnit"})
    List<SowResourceRequirement>
            findAllByOrderBySowIdAscPositionIdAscSkillIdAscSeniorityAscLocationAsc();

    @EntityGraph(attributePaths = {"sow", "sow.businessUnit"})
    List<SowResourceRequirement> findBySowId(Long sowId);

    void deleteBySowId(Long sowId);
}
