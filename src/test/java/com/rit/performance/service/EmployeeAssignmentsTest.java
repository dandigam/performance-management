package com.rit.performance.service;

import com.rit.performance.dto.EmployeeAssignmentResponse;
import com.rit.performance.dto.EmployeeBasicInfoResponse;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.Sow;
import com.rit.performance.repository.SowRepository;
import com.rit.performance.repository.SowMilestonePositionAssignmentRepository;
import com.rit.performance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeAssignmentsTest {
    @Mock SowRepository sowRepository;
    @Mock SowMilestonePositionAssignmentRepository milestonePositionAssignmentRepository;
    @Mock UserRepository userRepository;
    @Spy @InjectMocks EmployeeServiceImpl service;

    @Test
    void returnsAssignmentHistoryRegardlessOfParentAndMilestoneStatus() {
        var statuses = List.of("ASSIGNED", "ACTIVE", "COMPLETED", "INACTIVE");
        var parents = IntStream.range(0, statuses.size()).mapToObj(index ->
                EmployeeAssignmentResponse.builder().assignmentId((long) index + 1)
                        .sowId(10L).assignmentStatus(statuses.get(index)).build()).toList();
        doReturn(EmployeeBasicInfoResponse.builder().employeeId(2L)
                .assignmentList(parents).build()).when(service).getById(2L);
        var sow = new Sow();
        sow.setId(10L);
        var sowStatus = new LookupValue();
        sowStatus.setCode("ACTIVE");
        sow.setStatus(sowStatus);
        when(sowRepository.findAllById(any())).thenReturn(List.of(sow));
        var parent = new com.rit.performance.entity.EmployeeAssignment();
        parent.setId(3L);
        var milestone = new com.rit.performance.entity.SowMilestone();
        milestone.setId(20L);
        var position = new com.rit.performance.entity.SowMilestonePosition();
        position.setId(30L);
        position.setMilestone(milestone);
        var details = IntStream.range(0, statuses.size()).mapToObj(index -> {
            var detail = new com.rit.performance.entity.SowMilestonePositionAssignment();
            detail.setId((long) index + 100);
            detail.setEmployeeAssignment(parent);
            detail.setMilestonePosition(position);
            detail.setStatus(statuses.get(index));
            return detail;
        }).toList();
        when(milestonePositionAssignmentRepository
                .findByEmployeeAssignment_EmployeeIdOrderByAssignmentStartDateDescIdDesc(2L))
                .thenReturn(details);

        var response = service.getAssignmentsByEmployeeId(2L);

        assertThat(response.getAssignmentList()).extracting(item -> item.getAssignmentStatus())
                .containsExactlyElementsOf(statuses);
        var result = response.getAssignmentList().get(2);
        assertThat(result.getEmployeeAssignmentId()).isEqualTo(3L);
        assertThat(result.getAssignmentStatus()).isEqualTo("COMPLETED");
        assertThat(result.getMilestoneAssignments()).extracting(item -> item.getAssignmentId())
                .containsExactly(100L, 101L, 102L, 103L);
        assertThat(result.getMilestoneAssignments()).extracting(item -> item.getMilestonePositionAssignmentId())
                .containsExactly(100L, 101L, 102L, 103L);
        assertThat(result.getMilestoneAssignments()).extracting(item -> item.getAssignmentStatus())
                .containsExactlyElementsOf(statuses);
    }

    @Test
    void assignmentsIncludeDraftActiveAndCompletedSows() {
        var codes = List.of("DRAFT", "ACTIVE", "completed", "ON_HOLD", "CANCELLED", "APPROVED");
        var sows = IntStream.range(0, codes.size()).mapToObj(index -> {
            var status = new LookupValue();
            status.setCode(codes.get(index));
            var sow = new Sow();
            sow.setId((long) index + 1);
            sow.setStatus(status);
            return sow;
        }).toList();
        var assignments = sows.stream().map(sow -> EmployeeAssignmentResponse.builder()
                .assignmentId(sow.getId()).sowId(sow.getId())
                .assignmentStatus("ASSIGNED").build()).toList();
        doReturn(EmployeeBasicInfoResponse.builder().employeeId(3L)
                .employeeName("Employee").assignmentList(assignments).build())
                .when(service).getById(3L);
        when(sowRepository.findAllById(any())).thenReturn(sows);

        var response = service.getAssignmentsByEmployeeId(3L);

        assertThat(response.getAssignmentList()).extracting(item -> item.getSowId())
                .containsExactly(1L, 2L, 3L);
    }
}
