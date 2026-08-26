package com.rit.performance.repository;

import com.rit.performance.entity.SowMilestonePositionAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SowMilestonePositionAssignmentRepository
        extends JpaRepository<SowMilestonePositionAssignment, Long> {
    boolean existsByEmployeeAssignment_EmployeeIdAndMilestonePosition_Milestone_IdAndStatusIgnoreCase(
            Long employeeId, Long milestoneId, String status);

    boolean existsByEmployeeAssignment_IdAndStatusIgnoreCase(
            Long employeeAssignmentId, String status);

    boolean existsByMilestonePosition_Id(Long milestonePositionId);

    @EntityGraph(attributePaths = {"employeeAssignment", "milestonePosition",
            "milestonePosition.sow", "milestonePosition.milestone",
            "milestonePosition.position", "milestonePosition.rateCard"})
    List<SowMilestonePositionAssignment>
            findByMilestonePosition_IdOrderByAssignmentStartDateDescIdDesc(Long positionId);

    @EntityGraph(attributePaths = {"employeeAssignment", "milestonePosition",
            "milestonePosition.sow", "milestonePosition.milestone",
            "milestonePosition.position", "milestonePosition.rateCard"})
    List<SowMilestonePositionAssignment>
            findByMilestonePosition_Sow_IdAndStatusIgnoreCase(Long sowId, String status);

    @EntityGraph(attributePaths = {"employeeAssignment", "milestonePosition",
            "milestonePosition.sow", "milestonePosition.milestone",
            "milestonePosition.position", "milestonePosition.rateCard"})
    List<SowMilestonePositionAssignment>
            findByEmployeeAssignment_EmployeeIdOrderByAssignmentStartDateDescIdDesc(
                    Long employeeId);

    @EntityGraph(attributePaths = {"employeeAssignment", "milestonePosition",
            "milestonePosition.sow", "milestonePosition.milestone",
            "milestonePosition.position", "milestonePosition.rateCard"})
    Optional<SowMilestonePositionAssignment> findOneById(Long id);
}
